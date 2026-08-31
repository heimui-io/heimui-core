package io.heimui.core.presentation.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*

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

    fun interpolatePayload(payload: JsonObject?): JsonObject? {
        if (payload == null) return null
        val newMap = mutableMapOf<String, JsonElement>()

        payload.forEach { (key, value) ->
            if (value is JsonPrimitive && value.isString) {
                val strVal = value.content
                if (strVal.startsWith("{{state.") && strVal.endsWith("}}")) {
                    val stateKey = strVal.removePrefix("{{state.").removeSuffix("}}")
                    newMap[key] = JsonPrimitive(getValue(stateKey))
                } else {
                    newMap[key] = value
                }
            } else {
                newMap[key] = value
            }
        }
        return JsonObject(newMap)
    }
}
