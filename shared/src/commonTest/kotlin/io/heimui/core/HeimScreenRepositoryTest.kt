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
        cache.saveScreen("dashboard", cachedScreenDto, etag = "W/\"old-etag\"")

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

        // 3. Cache must be updated with Fresh ETag
        val updatedCache = cache.getScreen("dashboard")
        assertEquals("W/\"fresh-etag\"", updatedCache?.etag)
    }
}
