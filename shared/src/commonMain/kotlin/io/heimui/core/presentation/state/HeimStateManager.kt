package io.heimui.core.presentation.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface HeimStateStorage {
    fun save(screenId: String, state: Map<String, String>)
    fun load(screenId: String): Map<String, String>?
    fun clear(screenId: String)
}

class HeimStateManager(
    private val screenId: String,
    private val storage: HeimStateStorage? = null
) {
    private val _formState = MutableStateFlow<Map<String, String>>(
        storage?.load(screenId) ?: emptyMap()
    )
    val formState: StateFlow<Map<String, String>> = _formState.asStateFlow()

    fun updateValue(key: String, value: String) {
        val updated = _formState.value + (key to value)
        _formState.value = updated
        storage?.save(screenId, updated)
    }

    fun onNativeResult(key: String, value: String) {
        updateValue(key, value)
    }

    fun clearState() {
        _formState.value = emptyMap()
        storage?.clear(screenId)
    }

    fun getValue(key: String): String = _formState.value[key] ?: ""

    fun getAllValues(): Map<String, String> = _formState.value

    fun restoreDraft(draft: Map<String, String>) {
        val updated = _formState.value + draft
        _formState.value = updated
        storage?.save(screenId, updated)
    }

    fun interpolatePayload(payload: Map<String, Any?>?): Map<String, Any?>? {
        if (payload == null) return null
        val newMap = mutableMapOf<String, Any?>()

        payload.forEach { (key, value) ->
            if (value is String) {
                if (value.startsWith("{{state.") && value.endsWith("}}")) {
                    val stateKey = value.removePrefix("{{state.").removeSuffix("}}")
                    newMap[key] = getValue(stateKey)
                } else {
                    newMap[key] = value
                }
            } else {
                newMap[key] = value
            }
        }
        return newMap
    }
}
