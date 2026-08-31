package io.heimui.core.data.datasource.local

import io.heimui.core.data.dto.HeimScreenResponseDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CachedScreenEntry(
    val screen: HeimScreenResponseDto,
    val etag: String? = null,
    val cachedAtMillis: Long = 0L
)

/**
 * Low-level key-value storage driver contract.
 * Enables host apps to plug in SQLite, Room, DataStore, or encrypted key-value storage.
 */
interface HeimStorageDriver {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)
    suspend fun delete(key: String)
    suspend fun clear()
}

class InMemoryStorageDriver : HeimStorageDriver {
    private val memory = mutableMapOf<String, String>()
    override suspend fun get(key: String): String? = memory[key]
    override suspend fun put(key: String, value: String) { memory[key] = value }
    override suspend fun delete(key: String) { memory.remove(key) }
    override suspend fun clear() { memory.clear() }
}

interface HeimCacheDataSource {
    suspend fun getScreen(screenId: String): CachedScreenEntry?
    suspend fun saveScreen(screenId: String, screen: HeimScreenResponseDto, etag: String? = null)
    suspend fun clear(screenId: String)
    suspend fun clearAll()
}

class DriverBackedHeimCacheDataSource(
    private val driver: HeimStorageDriver = InMemoryStorageDriver(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
) : HeimCacheDataSource {

    override suspend fun getScreen(screenId: String): CachedScreenEntry? {
        val raw = driver.get(screenId) ?: return null
        return try {
            json.decodeFromString<CachedScreenEntry>(raw)
        } catch (_: Throwable) {
            null
        }
    }

    override suspend fun saveScreen(screenId: String, screen: HeimScreenResponseDto, etag: String?) {
        val entry = CachedScreenEntry(screen = screen, etag = etag, cachedAtMillis = 0L)
        val raw = json.encodeToString(entry)
        driver.put(screenId, raw)
    }

    override suspend fun clear(screenId: String) {
        driver.delete(screenId)
    }

    override suspend fun clearAll() {
        driver.clear()
    }
}

class InMemoryHeimCacheDataSource : HeimCacheDataSource {
    private val cache = mutableMapOf<String, CachedScreenEntry>()

    override suspend fun getScreen(screenId: String): CachedScreenEntry? {
        return cache[screenId]
    }

    override suspend fun saveScreen(screenId: String, screen: HeimScreenResponseDto, etag: String?) {
        cache[screenId] = CachedScreenEntry(
            screen = screen,
            etag = etag,
            cachedAtMillis = 0L
        )
    }

    override suspend fun clear(screenId: String) {
        cache.remove(screenId)
    }

    override suspend fun clearAll() {
        cache.clear()
    }
}
