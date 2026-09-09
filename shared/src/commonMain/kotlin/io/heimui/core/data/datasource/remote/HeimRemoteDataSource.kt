package io.heimui.core.data.datasource.remote

import io.heimui.core.data.dto.HeimScreenResponseDto
import io.heimui.core.data.resilience.HeimCircuitBreaker
import io.heimui.core.data.security.HeimPayloadGuard
import io.heimui.core.data.serialization.HeimJson
import io.heimui.core.domain.model.HeimValue
import io.heimui.core.domain.model.toJsonElement
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * What the SDK is about to request, so a host can decide whether to authenticate it.
 *
 * Screens and form submissions often live on different infrastructure: a CDN serving static
 * payloads and an API accepting writes. A single token for both is wrong in either direction —
 * send it to the CDN and you leak a credential to a third party (and some hosts, GitHub raw among
 * them, answer 404 to an Authorization header they cannot validate); withhold it from the API and
 * the submission is rejected.
 */
public sealed interface HeimAuthContext {
    /** The absolute URL about to be requested. */
    public val url: String

    /** Fetching a screen payload. Often a CDN, often unauthenticated. */
    public data class ScreenFetch(override val url: String) : HeimAuthContext

    /** Submitting a form. Usually your own API, usually authenticated. */
    public data class FormSubmit(override val url: String) : HeimAuthContext
}

/**
 * Supplies the `Authorization` header value, including its scheme (`"Bearer eyJ…"`).
 *
 * Called on every request, so a rotated token takes effect without reinitialising the SDK.
 * Return null — or a blank string — to send the request unauthenticated.
 */
public fun interface HeimAuthTokenProvider {
    public fun authHeaderFor(context: HeimAuthContext): String?
}

/** Raised when a payload asks the SDK to do something the security policy forbids. */
internal class HeimSecurityException(message: String) : IllegalStateException(message)

internal sealed interface RemoteScreenResponse {
  data class Success(
      val screen: HeimScreenResponseDto,
      val rawBytes: ByteArray,
      val etag: String?,
      val signature: String?,
  ) : RemoteScreenResponse

  data object NotModified : RemoteScreenResponse

  data class Error(val statusCode: Int, val message: String) : RemoteScreenResponse

  /** The circuit breaker is open; no request was issued. */
  data class CircuitOpen(val message: String) : RemoteScreenResponse
}

internal sealed interface RemoteSubmitResponse {
  data class Success(
      val responseScreen: HeimScreenResponseDto? = null,
      val rawJson: String? = null,
  ) : RemoteSubmitResponse

  data class Error(val statusCode: Int, val message: String) : RemoteSubmitResponse

  /** The payload violated the SDK security policy. Never retried, always surfaced. */
  data class SecurityViolation(val message: String) : RemoteSubmitResponse
}

/**
 * HTTP methods a server-driven payload is allowed to request. Anything else is refused so a hostile
 * payload cannot coerce the client into issuing, say, a DELETE against an arbitrary path.
 */
private val ALLOWED_SUBMIT_METHODS = setOf("POST", "PUT", "PATCH")

internal class HeimRemoteDataSource(
    private val httpClient: HttpClient = createDefaultHttpClient(),
    private val baseUrl: String,
    private val authTokenProvider: HeimAuthTokenProvider? = null,
    private val circuitBreaker: HeimCircuitBreaker = HeimCircuitBreaker(),
    /** Extra hosts allowed to receive authenticated form submissions, beyond the baseUrl host. */
    private val allowedSubmitHosts: Set<String> = emptySet(),
    /** Hosts reachable over cleartext `http://`, beyond loopback. See `HeimConfig`. */
    private val allowCleartextHosts: Set<String> = emptySet(),
) {

  suspend fun fetchScreen(
      screenId: String,
      queryParams: Map<String, String> = emptyMap(),
      ifNoneMatchEtag: String? = null,
  ): RemoteScreenResponse {
    // Resolved before the breaker, not inside it: a refused identifier is a payload or
    // configuration fault, and counting it as a network failure would let three bad ids pause
    // every screen in the app for the breaker's full interval.
    val screenUrl =
        try {
          resolveScreenUrl(screenId)
        } catch (e: HeimSecurityException) {
          return RemoteScreenResponse.Error(
              statusCode = -1,
              message = e.message ?: "Blocked by security policy",
          )
        }

    return try {
      circuitBreaker.withBreaker(
          onOpen = {
            RemoteScreenResponse.CircuitOpen(
                "Remote is unavailable; requests are paused to allow recovery."
            )
          }
      ) {
        performFetch(screenUrl, queryParams, ifNoneMatchEtag)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      RemoteScreenResponse.Error(statusCode = -1, message = e.message ?: "Network error")
    }
  }

  private suspend fun performFetch(
      screenUrl: String,
      queryParams: Map<String, String>,
      ifNoneMatchEtag: String?,
  ): RemoteScreenResponse {
    val response =
        httpClient.get(screenUrl) {
          headers {
            authTokenProvider
                ?.authHeaderFor(HeimAuthContext.ScreenFetch(screenUrl))
                ?.takeIf { it.isNotBlank() }
                ?.let { append(HttpHeaders.Authorization, it) }

          // Tells the backend who it is answering, so it can localise the payload. Localising on
          // the server beats shipping every translation to every device.
          heimDeviceLanguageTag()?.let { append(HttpHeaders.AcceptLanguage, it) }
            ifNoneMatchEtag?.let { append(HttpHeaders.IfNoneMatch, it) }
          }
          url { queryParams.forEach { (key, value) -> parameters.append(key, value) } }
        }

    return when (response.status) {
      HttpStatusCode.OK -> {
        val rawBytes = response.bodyAsBytes()
        if (rawBytes.size > HeimPayloadGuard.MAX_PAYLOAD_BYTES) {
          // Reported as a failure so repeated oversized responses trip the breaker.
          circuitBreaker.recordFailure()
          return RemoteScreenResponse.Error(
              statusCode = 413,
              message =
                  "Payload exceeds maximum allowed size of " +
                      "${HeimPayloadGuard.MAX_PAYLOAD_BYTES} bytes",
          )
        }
        // decodeScreen enforces the structural guard BEFORE the parser recurses.
        val screenDto = HeimJson.decodeScreen(rawBytes.decodeToString())
        RemoteScreenResponse.Success(
            screen = screenDto,
            rawBytes = rawBytes,
            etag = response.headers[HttpHeaders.ETag],
            signature =
                response.headers[SIGNATURE_HEADER] ?: response.headers[LEGACY_SIGNATURE_HEADER],
        )
      }

      HttpStatusCode.NotModified -> RemoteScreenResponse.NotModified

      else -> {
        if (response.status.value >= 500) circuitBreaker.recordFailure()
        RemoteScreenResponse.Error(
            statusCode = response.status.value,
            message =
                "Failed to fetch screen (HTTP ${response.status.value}" +
                    response.status.description
                        .takeIf { it.isNotBlank() }
                        ?.let { " $it" }
                        .orEmpty() +
                    ") from ${response.request.url}",
        )
      }
    }
  }

  suspend fun submitForm(
      endpoint: String,
      method: String = "POST",
      payload: Map<String, HeimValue>? = null,
  ): RemoteSubmitResponse {
    val secureUrl =
        try {
          resolveSecureUrl(endpoint)
        } catch (e: HeimSecurityException) {
          // A blocked exfiltration attempt is a result, not a crash: throwing here would turn
          // a hostile payload into a host-app crash, trading one vulnerability for another.
          return RemoteSubmitResponse.SecurityViolation(e.message ?: "Blocked by security policy")
        }

    val normalizedMethod = method.uppercase()
    if (normalizedMethod !in ALLOWED_SUBMIT_METHODS) {
      return RemoteSubmitResponse.SecurityViolation(
          "HTTP method '$normalizedMethod' is not permitted for form submission."
      )
    }

    return try {
      val response =
          httpClient.request(secureUrl) {
            this.method = HttpMethod.parse(normalizedMethod)
            headers {
              authTokenProvider
                  ?.authHeaderFor(HeimAuthContext.FormSubmit(secureUrl))
                  ?.takeIf { it.isNotBlank() }
                  ?.let { append(HttpHeaders.Authorization, it) }
              append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            if (payload != null) {
              val body = JsonObject(payload.mapValues { it.value.toJsonElement() })
              setBody(defaultJson.encodeToString(JsonObject.serializer(), body))
            }
          }

      if (response.status.isSuccess()) {
        val bodyText = response.bodyAsText()
        RemoteSubmitResponse.Success(
            responseScreen = HeimJson.decodeScreenOrNull(bodyText),
            rawJson = bodyText,
        )
      } else {
        RemoteSubmitResponse.Error(
            statusCode = response.status.value,
            message = "Form submission failed: ${response.status.description}",
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Throwable) {
      RemoteSubmitResponse.Error(statusCode = -1, message = e.message ?: "Network error")
    }
  }

  /**
   * Resolves a payload-supplied endpoint against the SDK's security policy.
   *
   * The `Authorization` header is attached to whatever URL this returns, so an unchecked absolute
   * endpoint is a direct session-token exfiltration channel. Host, port and scheme must all match
   * the configured origin (or an explicitly allow-listed host).
   */
  private fun resolveSecureUrl(endpoint: String): String {
    if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
      if (endpoint.contains("..")) {
        throw HeimSecurityException(
            "Path traversal is not permitted in form endpoints: '$endpoint'"
        )
      }
      return buildUrl(endpoint)
    }

    val target =
        runCatching { Url(endpoint) }
            .getOrElse { throw HeimSecurityException("Malformed form endpoint: '$endpoint'") }
    val base =
        runCatching { Url(baseUrl) }
            .getOrElse {
              throw HeimSecurityException(
                  "HeimUI is misconfigured: baseUrl '$baseUrl' is not a valid URL"
              )
            }

    if (target.protocol == URLProtocol.HTTP && !allowsCleartext(target.host)) {
      throw HeimSecurityException(
          "Cleartext HTTP form submission to '${target.host}' is prohibited. Add the host to " +
              "HeimConfig.allowCleartextHosts if this is a local development backend."
      )
    }

    val hostAllowed =
        target.host.equals(base.host, ignoreCase = true) ||
            allowedSubmitHosts.any { it.equals(target.host, ignoreCase = true) }
    if (!hostAllowed) {
      throw HeimSecurityException(
          "Cross-domain form submission to '${target.host}' is prohibited to prevent " +
              "credential exfiltration."
      )
    }

    // Same host on a different port is a different service; the token must not follow it there.
    val isAllowlistedHost = allowedSubmitHosts.any { it.equals(target.host, ignoreCase = true) }
    if (!isAllowlistedHost && target.port != base.port) {
      throw HeimSecurityException(
          "Form submission to '${target.host}:${target.port}' does not match the " +
              "configured origin port ${base.port}."
      )
    }

    return endpoint
  }

  /**
   * Whether cleartext `http://` is tolerated for [host].
   *
   * Loopback is always allowed, because that traffic never leaves the device. Everything else has
   * to be named in `HeimConfig.allowCleartextHosts` -- so the emulator alias a developer needs
   * (`10.0.2.2`) is a decision each app makes for its own debug build, not a hole the SDK opens
   * for every consumer. The platform still has to agree: Android needs `usesCleartextTraffic` (or
   * a network security config) and iOS an ATS exception, neither of which the SDK can grant.
   */
  private fun allowsCleartext(host: String): Boolean =
      host == "localhost" ||
          host == "127.0.0.1" ||
          allowCleartextHosts.any { it.equals(host, ignoreCase = true) }

  /**
   * Resolves a screen identifier into the URL it is fetched from.
   *
   * A relative id resolves against [baseUrl] -- `home`, `/home` and `catalog/detail` all work,
   * and the SDK contributes no path segment of its own, so the id names the backend's route
   * rather than a convention the SDK imposes on it.
   *
   * An absolute URL is accepted too, which is what lets one payload link to the next by href
   * instead of by an id the client must know how to expand. It is held to the same policy as a
   * form endpoint, because the `Authorization` header follows it: same host, same port, no
   * cleartext. Without that a hostile payload could name any URL and be handed the session token.
   *
   * @throws HeimSecurityException if the identifier resolves outside the configured origin. Every
   *   caller has to turn that into a result rather than let it escape -- see [fetchScreen] and
   *   [screenCacheKey].
   */
  internal fun resolveScreenUrl(screenId: String): String {
    // Compared lowercased: `HTTPS://evil.example` is absolute to every URL parser that matters,
    // and a case-sensitive check would have sent it down the relative branch.
    val scheme = screenId.substringBefore(':', "").lowercase()
    if (scheme != "http" && scheme != "https") {
      if (screenId.contains("..")) {
        throw HeimSecurityException(
            "Path traversal is not permitted in screen identifiers: '$screenId'"
        )
      }
      return buildUrl(screenId)
    }

    val target =
        runCatching { Url(screenId) }
            .getOrElse { throw HeimSecurityException("Malformed screen URL: '$screenId'") }
    val base =
        runCatching { Url(baseUrl) }
            .getOrElse {
              throw HeimSecurityException(
                  "HeimUI is misconfigured: baseUrl '$baseUrl' is not a valid URL"
              )
            }

    if (target.protocol == URLProtocol.HTTP && !allowsCleartext(target.host)) {
      throw HeimSecurityException(
          "Cleartext HTTP screen fetch from '${target.host}' is prohibited. Add the host to " +
              "HeimConfig.allowCleartextHosts if this is a local development backend."
      )
    }

    val isAllowlistedHost = allowedSubmitHosts.any { it.equals(target.host, ignoreCase = true) }
    if (!isAllowlistedHost && !target.host.equals(base.host, ignoreCase = true)) {
      throw HeimSecurityException(
          "Cross-domain screen fetch from '${target.host}' is prohibited."
      )
    }

    // Same host on a different port is a different service, exactly as it is for a submission.
    if (!isAllowlistedHost && target.port != base.port) {
      throw HeimSecurityException(
          "Screen fetch from '${target.host}:${target.port}' does not match the " +
              "configured origin port ${base.port}."
      )
    }

    return screenId
  }

  /**
   * The URL a screen resolves to, which is also what identifies it in the cache.
   *
   * Exposed so the repository keys its cache by exactly what it fetched, rather than by
   * `screenId` alone. Those are not the same thing: `product_detail?sku=x1` and `?sku=x2` share a
   * screen id and are different resources, and two origins can both serve a `products`. Keying by
   * the id collapsed all of them onto one entry, so opening a second product showed the first for
   * an instant — and sent the first one's ETag to ask about the second.
   */
  internal fun screenCacheKey(screenId: String, queryParams: Map<String, String>): String {
    // Deliberately does not propagate a refused identifier. The repository calls this outside any
    // catch, so throwing here would crash the collecting coroutine instead of surfacing an error
    // state; the refusal still happens, in [fetchScreen], which reports it as a result. The key
    // is only ever read in that case -- nothing is written under it, because the fetch fails.
    val base = runCatching { resolveScreenUrl(screenId) }.getOrElse { screenId }
    if (queryParams.isEmpty()) return base
    // Sorted, so two callers passing the same parameters in a different order share one entry
    // rather than quietly caching the same screen twice.
    val query = queryParams.entries
        .sortedBy { it.key }
        .joinToString("&") { "${it.key}=${it.value}" }
    return "$base?$query"
  }

  private fun buildUrl(path: String): String {
    val cleanBase = baseUrl.trimEnd('/')
    val cleanPath = if (path.startsWith("/")) path else "/$path"
    return cleanBase + cleanPath
  }

  companion object {
    const val SIGNATURE_HEADER: String = "X-Heim-Signature"
    const val LEGACY_SIGNATURE_HEADER: String = "X-Signature"

    val serializersModule = HeimJson.serializersModule

    /** Shared strict JSON configuration. See [HeimJson] for the rationale. */
    val defaultJson: Json = HeimJson.instance

    fun createDefaultHttpClient(
        engine: HttpClientEngine = createPlatformHttpClientEngine()
    ): HttpClient {
      return HttpClient(engine) {
        install(ContentNegotiation) { json(defaultJson) }
        // Without explicit timeouts a server that accepts the connection and never
        // responds leaves the coroutine hanging indefinitely.
        install(HttpTimeout) {
          requestTimeoutMillis = 15_000
          connectTimeoutMillis = 10_000
          socketTimeoutMillis = 10_000
        }
        install(HttpRequestRetry) {
          retryOnServerErrors(maxRetries = 2)
          exponentialDelay()
        }
      }
    }
  }
}
