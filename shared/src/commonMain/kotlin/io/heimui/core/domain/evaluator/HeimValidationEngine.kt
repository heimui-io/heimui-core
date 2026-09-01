package io.heimui.core.domain.evaluator

import io.heimui.core.domain.model.validation.ValidationRule
import io.heimui.core.domain.model.validation.ValidationType

public typealias CustomFieldValidator = (value: String, ruleValue: String?) -> Boolean

public class HeimValidatorRegistry {
    private val validators = mutableMapOf<String, CustomFieldValidator>()

    init {
        register("IBAN") { value, _ ->
            val clean = value.replace(WHITESPACE, "").uppercase()
            clean.length in 15..34 && clean.matches(IBAN_SHAPE)
        }
        register("LUHN") { value, _ ->
            val digits = value.filter { it.isDigit() }
            if (digits.length < 13) return@register false
            var sum = 0
            var alternate = false
            for (i in digits.length - 1 downTo 0) {
                var n = digits[i].digitToInt()
                if (alternate) {
                    n *= 2
                    if (n > 9) n -= 9
                }
                sum += n
                alternate = !alternate
            }
            sum % 10 == 0
        }
    }

    public fun register(name: String, validator: CustomFieldValidator): HeimValidatorRegistry {
        validators[name.uppercase()] = validator
        return this
    }

    public fun isRegistered(name: String): Boolean = validators.containsKey(name.uppercase())

    /**
     * Fail-closed: an unregistered validator name is a configuration error, and treating it as
     * "valid" would let a server-declared rule be silently skipped by an older client.
     *
     * [onMissingValidator] exists so the host learns about the misconfiguration instead of only
     * seeing users blocked by a rule nobody implemented.
     */
    public fun isValid(
        name: String,
        value: String,
        ruleValue: String?,
        onMissingValidator: (String) -> Unit = {}
    ): Boolean {
        val validator = validators[name.uppercase()]
        if (validator == null) {
            onMissingValidator(name)
            return false
        }
        return validator(value, ruleValue)
    }

    public companion object {
        public val default: HeimValidatorRegistry = HeimValidatorRegistry()
        private val WHITESPACE = Regex("\\s")
        private val IBAN_SHAPE = Regex("^[A-Z]{2}[0-9A-Z]+$")
    }
}

public object HeimValidationEngine {

    /**
     * Bounded LRU-ish cache of compiled patterns.
     *
     * Patterns come from the server, so an unbounded map is a slow memory leak: a backend that
     * generates per-user regexes would grow it without limit for the lifetime of the process.
     */
    private const val MAX_CACHED_PATTERNS = 64
    private const val MAX_PATTERN_LENGTH = 256
    private const val MAX_REGEX_INPUT_LENGTH = 4096

    private val regexCache = LinkedHashMap<String, Regex>()
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    private fun compiled(pattern: String): Regex? {
        regexCache[pattern]?.let { hit ->
            // Refresh recency.
            regexCache.remove(pattern)
            regexCache[pattern] = hit
            return hit
        }
        val compiled = runCatching { Regex(pattern) }.getOrNull() ?: return null
        if (regexCache.size >= MAX_CACHED_PATTERNS) {
            regexCache.keys.firstOrNull()?.let { regexCache.remove(it) }
        }
        regexCache[pattern] = compiled
        return compiled
    }

    public fun validate(
        value: String,
        rules: List<ValidationRule>,
        registry: HeimValidatorRegistry = HeimValidatorRegistry.default,
        onMissingValidator: (String) -> Unit = {}
    ): String? {
        for (rule in rules) {
            validateRule(value, rule, registry, onMissingValidator)?.let { return it }
        }
        return null
    }

    private fun validateRule(
        value: String,
        rule: ValidationRule,
        registry: HeimValidatorRegistry,
        onMissingValidator: (String) -> Unit
    ): String? {
        return when (rule.type) {
            ValidationType.REQUIRED ->
                if (value.trim().isEmpty()) rule.errorMessage else null

            ValidationType.REGEX -> {
                val pattern = rule.value ?: return rule.errorMessage
                // Both bounds blunt catastrophic backtracking: a hostile pattern needs both a
                // long pattern and a long subject to burn meaningful CPU.
                if (pattern.length > MAX_PATTERN_LENGTH) return rule.errorMessage
                if (value.length > MAX_REGEX_INPUT_LENGTH) return rule.errorMessage
                val regex = compiled(pattern) ?: return rule.errorMessage
                if (!regex.matches(value)) rule.errorMessage else null
            }

            ValidationType.MIN_LENGTH -> {
                val min = rule.value?.toIntOrNull() ?: 0
                if (value.length < min) rule.errorMessage else null
            }

            ValidationType.MAX_LENGTH -> {
                val max = rule.value?.toIntOrNull() ?: Int.MAX_VALUE
                if (value.length > max) rule.errorMessage else null
            }

            ValidationType.EMAIL ->
                if (!emailRegex.matches(value.trim())) rule.errorMessage else null

            ValidationType.NUMERIC ->
                if (value.isNotEmpty() && value.toDoubleOrNull() == null) rule.errorMessage else null

            ValidationType.CUSTOM -> {
                val customName = rule.value ?: return rule.errorMessage
                if (!registry.isValid(customName, value, rule.value, onMissingValidator)) {
                    rule.errorMessage
                } else {
                    null
                }
            }
        }
    }

    /**
     * Validates a whole form at once. Returns stateKey -> first error message.
     * Used to gate submission, which per-field validation alone cannot do: a field the user never
     * touched has never run its rules.
     */
    public fun validateAll(
        fields: Map<String, Pair<String, List<ValidationRule>>>,
        registry: HeimValidatorRegistry = HeimValidatorRegistry.default,
        onMissingValidator: (String) -> Unit = {}
    ): Map<String, String> = buildMap {
        fields.forEach { (stateKey, spec) ->
            val (value, rules) = spec
            validate(value, rules, registry, onMissingValidator)?.let { put(stateKey, it) }
        }
    }
}
