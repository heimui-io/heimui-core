package io.heimui.core.data.datasource.remote

import io.heimui.core.data.dto.HeimScreenResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

sealed interface RemoteScreenResponse {
    data class Success(val screen: HeimScreenResponseDto, val etag: String?) : RemoteScreenResponse
    data object NotModified : RemoteScreenResponse
    data class Error(val statusCode: Int, val message: String) : RemoteScreenResponse
}

sealed interface RemoteSubmitResponse {
    data class Success(val responseScreen: HeimScreenResponseDto? = null, val rawJson: String? = null) : RemoteSubmitResponse
    data class Error(val statusCode: Int, val message: String) : RemoteSubmitResponse
}

class HeimRemoteDataSource(
    private val httpClient: HttpClient = createDefaultHttpClient(),
    private val baseUrl: String,
    private val authTokenProvider: (() -> String?)? = null
) {

    suspend fun fetchScreen(
        screenId: String,
        queryParams: Map<String, String> = emptyMap(),
        ifNoneMatchEtag: String? = null
    ): RemoteScreenResponse {
        return try {
            val url = buildUrl("/screens/$screenId")
            val response = httpClient.get(url) {
                headers {
                    authTokenProvider?.invoke()?.let { token ->
                        append(HttpHeaders.Authorization, token)
                    }
                    ifNoneMatchEtag?.let { etag ->
                        append(HttpHeaders.IfNoneMatch, etag)
                    }
                }
                url {
                    queryParams.forEach { (key, value) ->
                        parameters.append(key, value)
                    }
                }
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val etag = response.headers[HttpHeaders.ETag]
                    val screenDto = response.body<HeimScreenResponseDto>()
                    RemoteScreenResponse.Success(screenDto, etag)
                }
                HttpStatusCode.NotModified -> {
                    RemoteScreenResponse.NotModified
                }
                else -> {
                    RemoteScreenResponse.Error(
                        statusCode = response.status.value,
                        message = "Failed to fetch screen: ${response.status.description}"
                    )
                }
            }
        } catch (e: Throwable) {
            RemoteScreenResponse.Error(
                statusCode = -1,
                message = e.message ?: "Network error"
            )
        }
    }

    suspend fun submitForm(
        endpoint: String,
        method: String = "POST",
        payload: Map<String, Any?>? = null
    ): RemoteSubmitResponse {
        return try {
            val url = if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
                endpoint
            } else {
                buildUrl(endpoint)
            }

            val httpMethod = HttpMethod.parse(method.uppercase())
            val response = httpClient.request(url) {
                this.method = httpMethod
                headers {
                    authTokenProvider?.invoke()?.let { token ->
                        append(HttpHeaders.Authorization, token)
                    }
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                if (payload != null) {
                    setBody(payload)
                }
            }

            if (response.status.isSuccess()) {
                val bodyText = response.bodyAsText()
                val responseScreen = try {
                    defaultJson.decodeFromString<HeimScreenResponseDto>(bodyText)
                } catch (_: Throwable) {
                    null
                }
                RemoteSubmitResponse.Success(responseScreen = responseScreen, rawJson = bodyText)
            } else {
                RemoteSubmitResponse.Error(
                    statusCode = response.status.value,
                    message = "Form submission failed: ${response.status.description}"
                )
            }
        } catch (e: Throwable) {
            RemoteSubmitResponse.Error(
                statusCode = -1,
                message = e.message ?: "Network error"
            )
        }
    }

    private fun buildUrl(path: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return cleanBase + cleanPath
    }

    companion object {
        val defaultJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        fun createDefaultHttpClient(engine: HttpClientEngine = createPlatformHttpClientEngine()): HttpClient {
            return HttpClient(engine) {
                install(ContentNegotiation) {
                    json(defaultJson)
                }
            }
        }
    }
}
