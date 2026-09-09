package io.heimui.core

import io.heimui.core.data.datasource.remote.HeimAuthContext
import io.heimui.core.data.datasource.remote.HeimRemoteDataSource
import io.heimui.core.data.datasource.remote.HeimSecurityException
import io.heimui.core.data.datasource.remote.RemoteScreenResponse
import io.heimui.core.data.datasource.remote.RemoteSubmitResponse
import io.heimui.core.di.HeimConfig
import io.heimui.core.di.cleartextHosts
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeimRemoteDataSourceTest {

    @Test
    fun testFetchScreenSuccessWithEtag() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("/home_screen", request.url.encodedPath)
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

        assertNull(seen["/home_screen"])
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

    @Test
    fun testFetchScreenSupportsSubpathsAndAbsoluteUrls() = runTest {
        val paths = mutableListOf<String>()
        val mockEngine = MockEngine { request ->
            paths.add(request.url.encodedPath)
            respond(
                content = """{"id":"test","root":{"type":"container","id":"c"}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()))
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val dataSource = HeimRemoteDataSource(
            httpClient = client,
            baseUrl = "https://api.heimui.io/api/sdui",
            allowedSubmitHosts = setOf("api.heimui.io")
        )

        // 1. Direct path without /screens/
        dataSource.fetchScreen("home")
        assertEquals("/api/sdui/home", paths[0])

        // 2. Relative path with subpath
        dataSource.fetchScreen("screens/catalog")
        assertEquals("/api/sdui/screens/catalog", paths[1])

        // 3. Absolute URL
        dataSource.fetchScreen("https://api.heimui.io/v2/special_screen")
        assertEquals("/v2/special_screen", paths[2])
    }

    @Test
    fun testFetchScreenBlocksPathTraversal() = runTest {
        val dataSource = HeimRemoteDataSource(
            httpClient = HttpClient(MockEngine { respond("{}") }),
            baseUrl = "https://api.heimui.io",
        )

        assertFailsWith<HeimSecurityException> {
            dataSource.resolveScreenUrl("../secrets")
        }

        val response = dataSource.fetchScreen("../secrets")
        assertIs<RemoteScreenResponse.Error>(response)
        assertTrue(response.message.contains("Path traversal"))
    }

    @Test
    fun testScreenCacheKeyNeverThrowsForARefusedIdentifier() {
        // The repository derives the cache key before it fetches, outside any catch. Throwing
        // here would crash the collecting coroutine instead of surfacing the refusal as a result.
        val dataSource = HeimRemoteDataSource(
            httpClient = HttpClient(MockEngine { respond("{}") }),
            baseUrl = "https://api.heimui.io",
        )

        assertEquals("../secrets", dataSource.screenCacheKey("../secrets", emptyMap()))
        assertEquals(
            "https://evil.example.com/x",
            dataSource.screenCacheKey("https://evil.example.com/x", emptyMap()),
        )
    }

    @Test
    fun testAbsoluteScreenUrlIsHeldToTheOriginPortAndScheme() = runTest {
        val dataSource = HeimRemoteDataSource(
            httpClient = HttpClient(MockEngine { respond("{}") }),
            baseUrl = "https://api.heimui.io",
        )

        // Same host on another port is another service, and the Authorization header follows the
        // URL -- the same rule a form submission is held to.
        assertFailsWith<HeimSecurityException> {
            dataSource.resolveScreenUrl("https://api.heimui.io:8443/home")
        }
        // Absolute is decided case-insensitively; upper case must not slip down the relative path.
        assertFailsWith<HeimSecurityException> {
            dataSource.resolveScreenUrl("HTTPS://evil.example.com/home")
        }
        assertFailsWith<HeimSecurityException> {
            dataSource.resolveScreenUrl("http://api.heimui.io/home")
        }
    }

    @Test
    fun testCleartextIsOffUntilTheHostIsDeclared() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"id":"home","root":{"type":"container","id":"c"}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()))
            )
        }

        // The Android emulator's alias for the host machine is a routable private address on a
        // real device, so it is a per-app decision rather than an SDK default.
        val closed = HeimRemoteDataSource(
            httpClient = HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            },
            baseUrl = "http://10.0.2.2:8080",
        )
        val refused = closed.fetchScreen("http://10.0.2.2:8080/home")
        assertIs<RemoteScreenResponse.Error>(refused)
        assertTrue(refused.message.contains("Cleartext"), refused.message)

        val opened = HeimRemoteDataSource(
            httpClient = HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            },
            baseUrl = "http://10.0.2.2:8080",
            allowCleartextHosts = setOf("10.0.2.2"),
        )
        assertIs<RemoteScreenResponse.Success>(opened.fetchScreen("http://10.0.2.2:8080/home"))
    }

    @Test
    fun testBaseUrlHostIsCleartextByDeclaration() {
        // The app wrote this origin in its own source; making it repeat itself in the allowlist
        // would buy no safety. A payload-named host still needs the entry.
        assertEquals(
            setOf("10.0.2.2"),
            HeimConfig(baseUrl = "http://10.0.2.2:8080").cleartextHosts(),
        )
        assertEquals(
            emptySet(),
            HeimConfig(baseUrl = "https://api.heimui.io").cleartextHosts(),
        )
        assertEquals(
            setOf("cdn.local"),
            HeimConfig(
                baseUrl = "https://api.heimui.io",
                allowCleartextHosts = setOf("cdn.local"),
            ).cleartextHosts(),
        )
    }

    @Test
    fun testARefusedIdentifierDoesNotTripTheBreaker() = runTest {
        var requests = 0
        val dataSource = HeimRemoteDataSource(
            httpClient = HttpClient(MockEngine {
                requests++
                respond(
                    content = """{"id":"home","root":{"type":"container","id":"c"}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()))
                )
            }) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            },
            baseUrl = "https://api.heimui.io",
        )

        // The breaker exists to spare a struggling backend, and a client-side refusal never
        // reached it. Three of them used to open it and pause every screen for its full interval.
        repeat(3) { assertIs<RemoteScreenResponse.Error>(dataSource.fetchScreen("../secrets")) }

        assertIs<RemoteScreenResponse.Success>(dataSource.fetchScreen("home"))
        assertEquals(1, requests)
    }

}
