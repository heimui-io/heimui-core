package io.heimui.core

import io.heimui.core.data.datasource.local.DriverBackedHeimCacheDataSource
import io.heimui.core.data.datasource.local.InMemoryHeimCacheDataSource
import io.heimui.core.data.dto.ContainerComponentDto
import io.heimui.core.data.dto.HeimScreenResponseDto
import io.heimui.core.data.dto.UnknownComponentDto
import io.heimui.core.domain.port.InMemoryStorageDriver
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
    @Test
    fun `the cache is bounded and drops the oldest first`() = runTest {
        val cache = DriverBackedHeimCacheDataSource(
            driver = InMemoryStorageDriver(),
            maxEntries = 3,
        )
        val screen = HeimScreenResponseDto(id = "s", root = UnknownComponentDto(id = "r"))

        repeat(4) { i -> cache.saveScreen("screen_$i", screen) }

        // Four saved, three kept: the first one out is the one written longest ago. Without a
        // bound a browsed catalogue leaves an entry per product, in storage the user never agreed
        // to spend, and a TTL alone does not limit how much accumulates before it expires.
        assertNull(cache.getScreen("screen_0"))
        assertNotNull(cache.getScreen("screen_1"))
        assertNotNull(cache.getScreen("screen_3"))
    }

    @Test
    fun `rewriting a screen does not consume another slot`() = runTest {
        val cache = DriverBackedHeimCacheDataSource(
            driver = InMemoryStorageDriver(),
            maxEntries = 2,
        )
        val screen = HeimScreenResponseDto(id = "s", root = UnknownComponentDto(id = "r"))

        cache.saveScreen("a", screen)
        cache.saveScreen("b", screen)
        // A screen revalidated on every open must not evict its neighbours simply for being
        // refreshed — that would make the most-used screens push each other out.
        cache.saveScreen("a", screen)

        assertNotNull(cache.getScreen("a"))
        assertNotNull(cache.getScreen("b"))
    }

}
