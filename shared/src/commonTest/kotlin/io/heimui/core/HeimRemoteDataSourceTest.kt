package io.heimui.core

import io.heimui.core.data.datasource.remote.HeimRemoteDataSource
import io.heimui.core.data.datasource.remote.RemoteScreenResponse
import io.heimui.core.data.datasource.remote.RemoteSubmitResponse
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HeimRemoteDataSourceTest {

    @Test
    fun testFetchScreenSuccessWithEtag() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("/screens/home_screen", request.url.encodedPath)
            respond(
                content = """{"id":"home_screen","root":{"type":"container","id":"c1"}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                    HttpHeaders.ETag to listOf("W/\"etag-99\"")
                )
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val dataSource = HeimRemoteDataSource(
            httpClient = client,
            baseUrl = "https://api.heimui.io"
        )

        val response = dataSource.fetchScreen("home_screen")
        assertIs<RemoteScreenResponse.Success>(response)
        assertEquals("home_screen", response.screen.id)
        assertEquals("W/\"etag-99\"", response.etag)
    }

    @Test
    fun testFetchScreenNotModified() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("W/\"etag-99\"", request.headers[HttpHeaders.IfNoneMatch])
            respond(
                content = "",
                status = HttpStatusCode.NotModified
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val dataSource = HeimRemoteDataSource(
            httpClient = client,
            baseUrl = "https://api.heimui.io"
        )

        val response = dataSource.fetchScreen("home_screen", ifNoneMatchEtag = "W/\"etag-99\"")
        assertIs<RemoteScreenResponse.NotModified>(response)
    }

    @Test
    fun testSubmitFormSuccess() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("/api/v1/checkout", request.url.encodedPath)
            respond(
                content = """{"id":"success_screen","root":{"type":"container","id":"c2"}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()))
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val dataSource = HeimRemoteDataSource(
            httpClient = client,
            baseUrl = "https://api.heimui.io"
        )

        val response = dataSource.submitForm(
            endpoint = "/api/v1/checkout",
            method = "POST",
            payload = mapOf("amount" to 5000)
        )

        assertIs<RemoteSubmitResponse.Success>(response)
        assertEquals("success_screen", response.responseScreen?.id)
    }
}
