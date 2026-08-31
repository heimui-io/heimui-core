package io.heimui.core.data.datasource.local

import io.heimui.core.data.dto.HeimScreenResponseDto

data class CachedScreenEntry(
    val screen: HeimScreenResponseDto,
    val etag: String? = null,
    val cachedAtMillis: Long = 0L
)

interface HeimCacheDataSource {
    suspend fun getScreen(screenId: String): CachedScreenEntry?
    suspend fun saveScreen(screenId: String, screen: HeimScreenResponseDto, etag: String? = null)
    suspend fun clear(screenId: String)
    suspend fun clearAll()
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
