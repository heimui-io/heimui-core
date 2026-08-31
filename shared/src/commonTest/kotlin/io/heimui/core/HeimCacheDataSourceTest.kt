package io.heimui.core

import io.heimui.core.data.datasource.local.InMemoryHeimCacheDataSource
import io.heimui.core.data.dto.ContainerComponentDto
import io.heimui.core.data.dto.HeimScreenResponseDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HeimCacheDataSourceTest {

    @Test
    fun testSaveAndRetrieveCachedScreen() = runTest {
        val cache = InMemoryHeimCacheDataSource()
        val dummyScreen = HeimScreenResponseDto(
            id = "screen_1",
            root = ContainerComponentDto(id = "c1")
        )

        assertNull(cache.getScreen("screen_1"))

        cache.saveScreen("screen_1", dummyScreen, etag = "W/\"12345\"")

        val retrieved = cache.getScreen("screen_1")
        assertNotNull(retrieved)
        assertEquals("screen_1", retrieved.screen.id)
        assertEquals("W/\"12345\"", retrieved.etag)

        cache.clear("screen_1")
        assertNull(cache.getScreen("screen_1"))
    }
}
