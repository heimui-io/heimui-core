package io.heimui.core

import io.heimui.core.data.datasource.remote.HeimAuthContext
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
import kotlin.test.assertNull

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
            payload = mapOf("amount" to io.heimui.core.domain.model.HeimValue.Num(5000.0))
        )

        assertIs<RemoteSubmitResponse.Success>(response)
        assertEquals("success_screen", response.responseScreen?.id)
    }

    @Test
    fun `auth header follows the context and not the request`() = runTest {
        val seen = mutableMapOf<String, String?>()
        val mockEngine = MockEngine { request ->
            seen[request.url.encodedPath] = request.headers[HttpHeaders.Authorization]
            respond(
                content = """{"id":"home_screen","root":{"type":"container","id":"c1"}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString())
                )
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val dataSource = HeimRemoteDataSource(
            httpClient = client,
            baseUrl = "https://api.heimui.io",
            allowedSubmitHosts = setOf("api.heimui.io"),
            // The whole point of handing the provider a context: screens live on a CDN that has no
            // business seeing a credential, while writes go to our own API and must carry it.
            authTokenProvider = { context ->
                when (context) {
                    is HeimAuthContext.ScreenFetch -> null
                    is HeimAuthContext.FormSubmit -> "Bearer submit-only"
                }
            }
        )

        dataSource.fetchScreen("home_screen")
        dataSource.submitForm("https://api.heimui.io/kyc", "POST", emptyMap())

        assertNull(seen["/screens/home_screen"])
        assertEquals("Bearer submit-only", seen["/kyc"])
    }

    @Test
    fun `a blank token is treated as no token`() = runTest {
        var header: String? = "unset"
        val mockEngine = MockEngine { request ->
            header = request.headers[HttpHeaders.Authorization]
            respond(
                content = """{"id":"s","root":{"type":"container","id":"c1"}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString())
                )
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        // A provider that returns "" while the user is signed out must not send `Authorization: `,
        // which some gateways reject outright rather than ignoring.
        HeimRemoteDataSource(
            httpClient = client,
            baseUrl = "https://api.heimui.io",
            authTokenProvider = { "   " }
        ).fetchScreen("s")

        assertNull(header)
    }

}
