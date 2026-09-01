package io.heimui.core.data.datasource.local

import io.heimui.core.data.dto.HeimScreenResponseDto
import io.heimui.core.data.serialization.HeimJson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Circuit Breaker provider for offline fallback emergency bundles.
 * Used when the remote network fails and there is no local cache available.
 */
public interface HeimEmergencyBundleProvider {
    public suspend fun getEmergencyScreen(screenId: String): HeimScreenResponseDto?
}

/**
 * Default implementation supporting pre-bundled JSON fallback templates.
 */
public class DefaultHeimEmergencyBundleProvider(
    private val bundles: Map<String, String> = emptyMap()
) : HeimEmergencyBundleProvider {

    private val parsedScreens = mutableMapOf<String, HeimScreenResponseDto>()
    private val mutex = Mutex()

    override suspend fun getEmergencyScreen(screenId: String): HeimScreenResponseDto? =
        mutex.withLock {
            parsedScreens[screenId]?.let { return@withLock it }
            val rawJson = bundles[screenId] ?: return@withLock null
            // Bundles ship with the app, but they are still parsed by the same recursive parser,
            // so they go through the same structural guard as network payloads.
            HeimJson.decodeScreenOrNull(rawJson)?.also { parsedScreens[screenId] = it }
        }
}
