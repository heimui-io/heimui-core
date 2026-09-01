package io.heimui.core.presentation.state

import io.heimui.core.domain.port.HeimStorageDriver
import io.heimui.core.domain.port.InMemoryStorageDriver
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Storage manager for persisting in-progress form drafts across app crashes or process deaths.
 */
public interface HeimFormDraftStorage {
    public suspend fun saveDraft(screenId: String, state: Map<String, String>)
    public suspend fun getDraft(screenId: String): Map<String, String>?
    public suspend fun clearDraft(screenId: String)
}

public class DriverBackedFormDraftStorage(
    private val driver: HeimStorageDriver = InMemoryStorageDriver(),
    private val json: Json = io.heimui.core.data.serialization.HeimJson.instance
) : HeimFormDraftStorage {

    private fun keyFor(screenId: String) = "heim_form_draft_$screenId"

    override suspend fun saveDraft(screenId: String, state: Map<String, String>) {
        if (state.isEmpty()) {
            clearDraft(screenId)
            return
        }
        val raw = json.encodeToString(state)
        driver.put(keyFor(screenId), raw)
    }

    override suspend fun getDraft(screenId: String): Map<String, String>? {
        val raw = driver.get(keyFor(screenId)) ?: return null
        return try {
            json.decodeFromString<Map<String, String>>(raw)
        } catch (_: Throwable) {
            null
        }
    }

    override suspend fun clearDraft(screenId: String) {
        driver.delete(keyFor(screenId))
    }
}
