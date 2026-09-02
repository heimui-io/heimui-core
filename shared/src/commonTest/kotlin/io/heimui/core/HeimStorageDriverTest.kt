package io.heimui.core

import io.heimui.core.data.datasource.local.DriverBackedHeimCacheDataSource
import io.heimui.core.domain.port.HeimStorageDriver
import io.heimui.core.data.dto.ContainerComponentDto
import io.heimui.core.data.dto.HeimScreenResponseDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HeimStorageDriverTest {

    @Test
    fun `test custom storage driver persistence`() = runTest {
        val mockDisk = mutableMapOf<String, String>()
        val customDriver = object : HeimStorageDriver {
            override suspend fun get(key: String): String? = mockDisk[key]
            override suspend fun put(key: String, value: String) { mockDisk[key] = value }
            override suspend fun delete(key: String) { mockDisk.remove(key) }
            override suspend fun clear() { mockDisk.clear() }
        }

        val cacheDataSource = DriverBackedHeimCacheDataSource(driver = customDriver)

        val screenDto = HeimScreenResponseDto(
            id = "profile_screen",
            version = "1.0.0",
            title = "Profile",
            root = ContainerComponentDto(id = "root_container")
        )

        // 1. Save screen
        cacheDataSource.saveScreen("profile_screen", screenDto, etag = "etag_abc123")
        // Two keys: the screen, and the index the cache keeps to know what to evict first. The
        // driver interface has no way to enumerate keys, so the bound is tracked in a document of
        // its own rather than by asking every implementor for a listing API.
        assertEquals(2, mockDisk.size)

        // 2. Retrieve screen
        val cached = cacheDataSource.getScreen("profile_screen")
        assertNotNull(cached)
        assertEquals("profile_screen", cached.screen.id)
        assertEquals("etag_abc123", cached.etag)

        // 3. Clear single
        cacheDataSource.clear("profile_screen")
        assertNull(cacheDataSource.getScreen("profile_screen"))
        assertEquals(0, mockDisk.size)
    }
}
