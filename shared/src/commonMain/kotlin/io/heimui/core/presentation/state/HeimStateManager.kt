package io.heimui.core.presentation.state

import io.heimui.core.domain.evaluator.HeimValidationEngine
import io.heimui.core.domain.evaluator.HeimValidatorRegistry
import io.heimui.core.domain.model.HeimValue
import io.heimui.core.domain.model.validation.ValidationRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Reactive form state for a single screen.
 *
 * Persistence is delegated to [HeimFormDraftStorage] (a suspend contract) and performed off the
 * caller's thread with a debounce, because this class is driven from `onValueChange` on every
 * keystroke -- synchronous disk I/O there is an ANR.
 */
public class HeimStateManager(
    private val screenId: String,
    private val draftStorage: HeimFormDraftStorage? = null,
    private val scope: CoroutineScope? = null,
    private val screenVersion: String = "1.0.0",
    private val draftDebounceMillis: Long = 400L
) {
    private val _formState = MutableStateFlow<Map<String, String>>(emptyMap())
    public val formState: StateFlow<Map<String, String>> = _formState.asStateFlow()

    /** Keys excluded from persistence (passwords, and anything the host marks sensitive). */
    private val sensitiveKeys = mutableSetOf<String>()

    /**
     * Validation rules declared by the fields currently on screen.
     *
     * Needed because per-field validation only runs on typing: a required field the user never
     * touched has never evaluated its rules, so submission could not be gated without this.
     */
    private val fieldRules = mutableMapOf<String, List<ValidationRule>>()

    private val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())

    /** Errors produced by the last submit-time validation, keyed by stateKey. */
    public val fieldErrors: StateFlow<Map<String, String>> = _fieldErrors.asStateFlow()

    private var draftJob: Job? = null

    /** Registers a field's rules. Called by the renderers as they enter composition. */
    public fun registerField(stateKey: String, rules: List<ValidationRule>) {
        if (rules.isEmpty()) return
        fieldRules[stateKey] = rules
    }

    public fun unregisterField(stateKey: String) {
        fieldRules.remove(stateKey)
    }

    /**
     * Validates every registered field. Returns stateKey -> error, empty when the form is valid.
     * Results are published to [fieldErrors] so inputs can surface them without being touched.
     */
    public fun validateForm(
        registry: HeimValidatorRegistry = HeimValidatorRegistry.default,
        onMissingValidator: (String) -> Unit = {}
    ): Map<String, String> {
        val snapshot = _formState.value
        val errors = HeimValidationEngine.validateAll(
            fields = fieldRules.mapValues { (key, rules) -> (snapshot[key] ?: "") to rules },
            registry = registry,
            onMissingValidator = onMissingValidator
        )
        _fieldErrors.value = errors
        return errors
    }

    public fun clearFieldError(stateKey: String) {
        if (_fieldErrors.value.containsKey(stateKey)) {
            _fieldErrors.update { it - stateKey }
        }
    }

    /**
     * Atomic read-modify-write.
     *
     * `_formState.value = _formState.value + pair` is a lost-update race: two concurrent writers
     * can interleave between the read and the write and silently drop one of the values.
     */
    public fun updateValue(key: String, value: String) {
        _formState.update { it + (key to value) }
        scheduleDraftSave()
    }

    public fun updateValues(values: Map<String, String>) {
        if (values.isEmpty()) return
        _formState.update { it + values }
        scheduleDraftSave()
    }

    /** Marks [key] as sensitive so it is never written to draft storage. */
    public fun markSensitive(key: String) {
        sensitiveKeys += key
    }

    public fun onNativeResult(key: String, value: String): Unit = updateValue(key, value)

    public fun clearState() {
        _formState.value = emptyMap()
        draftJob?.cancel()
        scope?.launch { draftStorage?.clearDraft(screenId) }
    }

    public fun getValue(key: String): String = _formState.value[key] ?: ""

    public fun hasValue(key: String): Boolean = _formState.value.containsKey(key)

    public fun getAllValues(): Map<String, String> = _formState.value

    public fun restoreDraft(draft: Map<String, String>) {
        if (draft.isEmpty()) return
        // Existing user input wins over a restored draft: the draft is older by definition.
        _formState.update { draft + it }
    }

    /** Persists the current draft immediately, e.g. from a lifecycle onStop callback. */
    public suspend fun flushDraft() {
        draftJob?.cancel()
        persistDraft()
    }

    private fun scheduleDraftSave() {
        if (draftStorage == null) return
        val activeScope = scope ?: return
        draftJob?.cancel()
        draftJob = activeScope.launch(Dispatchers.Default) {
            delay(draftDebounceMillis)
            persistDraft()
        }
    }

    private suspend fun persistDraft() {
        val storage = draftStorage ?: return
        val persistable = _formState.value.filterKeys { it !in sensitiveKeys && it != DRAFT_VERSION_KEY }
        if (persistable.isEmpty()) {
            storage.clearDraft(screenId)
        } else {
            // Stamp the version so a draft is never restored onto a screen whose keys have moved.
            storage.saveDraft(screenId, persistable + (DRAFT_VERSION_KEY to screenVersion))
        }
    }

    /**
     * Resolves `{{state.key}}` placeholders inside a submit payload.
     *
     * Handles three cases the previous implementation did not: placeholders embedded in a longer
     * string, placeholders nested inside objects and arrays, and a missing key -- which now
     * surfaces through [onUnresolved] instead of silently becoming an empty string. Sending
     * `amount: ""` because a state key was misspelled is not an acceptable failure mode for a
     * financial form.
     */
    public fun interpolatePayload(
        payload: Map<String, HeimValue>?,
        onUnresolved: (key: String) -> Unit = {}
    ): Map<String, HeimValue>? {
        if (payload == null) return null
        return payload.mapValues { (_, value) -> interpolateValue(value, onUnresolved) }
    }

    private fun interpolateValue(
        value: HeimValue,
        onUnresolved: (String) -> Unit
    ): HeimValue = when (value) {
        is HeimValue.Str -> interpolateString(value.value, onUnresolved)
        is HeimValue.Arr -> HeimValue.Arr(value.items.map { interpolateValue(it, onUnresolved) })
        is HeimValue.Obj -> HeimValue.Obj(value.fields.mapValues { interpolateValue(it.value, onUnresolved) })
        else -> value
    }

    /**
     * Substitutes `{{state.key}}` placeholders.
     *
     * Hand-written scanner rather than a regex on purpose. The three targets do not share a regex
     * engine -- Android runs ICU, the JVM runs java.util.regex, Kotlin/Native has its own -- and
     * they disagree on details as small as whether a bare `}` is a literal. A pattern that
     * compiled fine in unit tests on two of them crashed at startup on the third. Plain string
     * scanning behaves identically everywhere and is faster besides.
     */
    private fun interpolateString(raw: String, onUnresolved: (String) -> Unit): HeimValue {
        if (!raw.contains(PLACEHOLDER_OPEN)) return HeimValue.Str(raw)

        val trimmed = raw.trim()
        // The whole value is a single placeholder: resolve it preserving its natural type.
        if (trimmed.startsWith(PLACEHOLDER_OPEN) && trimmed.endsWith(PLACEHOLDER_CLOSE)) {
            val inner = trimmed.substring(PLACEHOLDER_OPEN.length, trimmed.length - PLACEHOLDER_CLOSE.length)
            if (PLACEHOLDER_OPEN !in inner && "}" !in inner) {
                val key = inner.trim().removePrefix(STATE_PREFIX)
                val resolved = _formState.value[key]
                    ?: return HeimValue.Null.also { onUnresolved(key) }
                return coerce(resolved)
            }
        }

        // Embedded placeholders: the result is necessarily a string.
        val out = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val open = raw.indexOf(PLACEHOLDER_OPEN, i)
            if (open < 0) {
                out.append(raw, i, raw.length)
                break
            }
            val close = raw.indexOf(PLACEHOLDER_CLOSE, open + PLACEHOLDER_OPEN.length)
            if (close < 0) {
                // Unterminated placeholder: emit the rest verbatim rather than dropping it.
                out.append(raw, i, raw.length)
                break
            }
            out.append(raw, i, open)
            val key = raw.substring(open + PLACEHOLDER_OPEN.length, close).trim().removePrefix(STATE_PREFIX)
            out.append(_formState.value[key] ?: run { onUnresolved(key); "" })
            i = close + PLACEHOLDER_CLOSE.length
        }
        return HeimValue.Str(out.toString())
    }

    /**
     * Form state is stored as text, so a placeholder that fills a whole value has to recover the
     * intended type. Long is tried before Double so an integer keeps its exact value.
     */
    private fun coerce(value: String): HeimValue = when {
        value.equals("true", ignoreCase = true) -> HeimValue.Bool(true)
        value.equals("false", ignoreCase = true) -> HeimValue.Bool(false)
        value.toLongOrNull() != null -> HeimValue.Int64(value.toLong())
        value.toDoubleOrNull() != null -> HeimValue.Num(value.toDouble())
        else -> HeimValue.Str(value)
    }

    private companion object {
        const val PLACEHOLDER_OPEN = "{{"
        const val PLACEHOLDER_CLOSE = "}}"
        const val STATE_PREFIX = "state."
    }
}
