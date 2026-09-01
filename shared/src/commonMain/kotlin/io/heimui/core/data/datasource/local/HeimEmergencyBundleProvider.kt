package io.heimui.core.data.datasource.local

import io.heimui.core.data.dto.HeimScreenResponseDto
import kotlinx.serialization.json.Json

/**
 * Circuit Breaker provider for offline fallback emergency bundles.
 * Used when the remote network fails and there is no local cache available.
 */
interface HeimEmergencyBundleProvider {
    suspend fun getEmergencyScreen(screenId: String): HeimScreenResponseDto?
}

/**
 * Default implementation supporting pre-bundled JSON fallback templates.
 */
class DefaultHeimEmergencyBundleProvider(
    private val bundles: Map<String, String> = emptyMap(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) : HeimEmergencyBundleProvider {

    private val parsedScreens = mutableMapOf<String, HeimScreenResponseDto>()

    override suspend fun getEmergencyScreen(screenId: String): HeimScreenResponseDto? {
        val cached = parsedScreens[screenId]
        if (cached != null) return cached

        val rawJson = bundles[screenId] ?: return null
        return try {
            val screen = json.decodeFromString<HeimScreenResponseDto>(rawJson)
            parsedScreens[screenId] = screen
            screen
        } catch (_: Throwable) {
            null
        }
    }
}
