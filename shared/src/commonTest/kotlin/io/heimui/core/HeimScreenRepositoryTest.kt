package io.heimui.core

import io.heimui.core.data.datasource.local.InMemoryHeimCacheDataSource
import io.heimui.core.data.datasource.remote.HeimRemoteDataSource
import io.heimui.core.data.dto.ContainerComponentDto
import io.heimui.core.data.dto.HeimScreenResponseDto
import io.heimui.core.data.dto.TextComponentDto
import io.heimui.core.data.repository.HeimScreenRepositoryImpl
import io.heimui.core.domain.repository.HeimScreenResult
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeimScreenRepositoryTest {

    @Test
    fun testStaleWhileRevalidateFlow() = runTest {
        val cache = InMemoryHeimCacheDataSource()
        val cachedScreenDto = HeimScreenResponseDto(
            id = "dashboard",
            title = "Cached Dashboard",
            root = ContainerComponentDto(
                id = "c1",
                children = listOf(TextComponentDto(id = "t1", text = "Old Cached Data"))
            )
        )
        // Keyed by the URL, not the screen id: `product_detail?sku=x1` and `?sku=x2` share an id and
        // are different resources, so the id alone collapsed them onto one entry.
        cache.saveScreen(
            "https://api.heimui.io/screens/dashboard",
            cachedScreenDto,
            etag = "W/\"old-etag\"",
        )

        val mockEngine = MockEngine { request ->
            assertEquals("W/\"old-etag\"", request.headers[HttpHeaders.IfNoneMatch])
            respond(
                content = """
                {
                    "id": "dashboard",
                    "title": "Fresh Dashboard",
                    "root": {
                        "type": "container",
                        "id": "c1",
                        "children": [{"type": "text", "id": "t1", "text": "Fresh Remote Data"}]
                    }
                }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                    HttpHeaders.ETag to listOf("W/\"fresh-etag\"")
                )
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(HeimRemoteDataSource.defaultJson)
            }
        }

        val remoteDataSource = HeimRemoteDataSource(
            httpClient = client,
            baseUrl = "https://api.heimui.io"
        )

        val repository = HeimScreenRepositoryImpl(
            remoteDataSource = remoteDataSource,
            cacheDataSource = cache
        )

        val results = repository.getScreen("dashboard").toList()

        // 1. Must emit Cached Content first (Stale)
        assertEquals(2, results.size)
        assertIs<HeimScreenResult.Success>(results[0])
        val staleResult = results[0] as HeimScreenResult.Success
        assertEquals("Cached Dashboard", staleResult.screen.title)
        assertTrue(staleResult.isStale)

        // 2. Must emit Fresh Remote Content second (Not Stale)
        assertIs<HeimScreenResult.Success>(results[1])
        val freshResult = results[1] as HeimScreenResult.Success
        assertEquals("Fresh Dashboard", freshResult.screen.title)
        assertFalse(freshResult.isStale)

        // 3. Cache must be updated with Fresh ETag, under the same URL it was fetched from.
        val updatedCache = cache.getScreen("https://api.heimui.io/screens/dashboard")
        assertEquals("W/\"fresh-etag\"", updatedCache?.etag)

        // And nothing under the bare screen id: keying by that is what let two products share one
        // entry, so opening the second showed the first and asked about it with the first's ETag.
        assertNull(cache.getScreen("dashboard"))
    }
    @Test
    fun `two screens differing only by query parameters do not share a cache entry`() = runTest {
        val cache = InMemoryHeimCacheDataSource()
        var served = 0

        val mockEngine = MockEngine { request ->
            served++
            val sku = request.url.parameters["sku"]
            respond(
                content = """{"id":"product_detail","title":"Product $sku",
                              "root":{"type":"container","id":"c"}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString())
                )
            )
        }
        val repository = HeimScreenRepositoryImpl(
            remoteDataSource = HeimRemoteDataSource(
                httpClient = HttpClient(mockEngine) {
                    install(ContentNegotiation) { json(HeimRemoteDataSource.defaultJson) }
                },
                baseUrl = "https://api.heimui.io",
            ),
            cacheDataSource = cache,
        )

        val first = repository.getScreen("product_detail", mapOf("sku" to "x1")).toList()
        val second = repository.getScreen("product_detail", mapOf("sku" to "x2")).toList()

        // The second product must not open showing the first. Keyed by screen id alone they shared
        // one entry, so the cache answered the x2 request with x1's content — a visible flash of
        // the wrong product on every list-to-detail navigation.
        assertEquals(1, first.size)
        assertEquals(1, second.size)
        assertEquals("Product x1", assertIs<HeimScreenResult.Success>(first.single()).screen.title)
        assertEquals("Product x2", assertIs<HeimScreenResult.Success>(second.single()).screen.title)
        assertEquals(2, served)

        // Both are cached, under the URL each was fetched from.
        assertEquals(
            "product_detail",
            cache.getScreen("https://api.heimui.io/screens/product_detail?sku=x1")?.screen?.id
        )
        assertNotNull(cache.getScreen("https://api.heimui.io/screens/product_detail?sku=x2"))
    }

    @Test
    fun `parameter order does not create a second entry for the same screen`() = runTest {
        val remote = HeimRemoteDataSource(
            httpClient = HttpClient(MockEngine { respond("{}") }),
            baseUrl = "https://api.heimui.io",
        )

        // Two callers passing the same filters in a different order are asking for the same thing.
        // Without sorting they would quietly cache it twice and revalidate it twice.
        assertEquals(
            remote.screenCacheKey("search", mapOf("q" to "shoes", "page" to "2")),
            remote.screenCacheKey("search", mapOf("page" to "2", "q" to "shoes")),
        )
    }

}
