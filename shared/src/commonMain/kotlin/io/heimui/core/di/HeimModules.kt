package io.heimui.core.di

import io.heimui.core.data.datasource.local.HeimCacheDataSource
import io.heimui.core.data.datasource.local.HeimEmergencyBundleProvider
import io.heimui.core.data.datasource.local.InMemoryHeimCacheDataSource
import io.heimui.core.data.datasource.remote.HeimAuthTokenProvider
import io.heimui.core.data.datasource.remote.HeimRemoteDataSource
import io.heimui.core.data.repository.HeimScreenRepositoryImpl
import io.heimui.core.data.security.DefaultHeimSignatureVerifier
import io.heimui.core.data.security.Es256SignatureVerifier
import io.heimui.core.data.security.HeimSignatureVerifier
import io.heimui.core.domain.repository.HeimScreenRepository
import io.ktor.client.HttpClient
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Configuration for a HeimUI instance.
 *
 * Passed once to [io.heimui.core.HeimUI.initialize]. Every field except [baseUrl] has a working
 * default, so the minimal setup is:
 *
 * ```kotlin
 * HeimUI.initialize(
 *     HeimConfig(
 *         baseUrl = "https://api.example.com/sdui",
 *         authTokenProvider = { session.bearerToken },
 *     )
 * )
 * ```
 *
 * @property baseUrl Origin the SDK fetches screens from. Relative screens resolve against
 *   `$baseUrl/{id}`. Also defines the trust boundary for form submissions: a payload
 *   cannot submit to another host unless it is listed in `allowedSubmitHosts`.
 * @property authTokenProvider Supplies the `Authorization` header, per request.
 *
 *   Receives a [io.heimui.core.data.datasource.remote.HeimAuthContext] naming what is about to be
 *   requested, so screens and submissions can be authenticated differently — which matters when
 *   payloads come from a CDN and forms go to your API:
 *
 *   ```kotlin
 *   authTokenProvider = HeimAuthTokenProvider { context ->
 *       when (context) {
 *           is HeimAuthContext.ScreenFetch -> null            // public CDN
 *           is HeimAuthContext.FormSubmit -> session.bearer()  // your API
 *       }
 *   }
 *   ```
 * @property allowedSubmitHosts Extra hosts permitted to receive authenticated form submissions.
 *   Empty by default: only `baseUrl`'s own origin is allowed, which is what prevents a malicious
 *   payload from exfiltrating the session token to a third party.
 * @property allowCleartextHosts Hosts the SDK may reach over cleartext `http://`, for screens and
 *   for form submissions alike. Loopback (`localhost`, `127.0.0.1`) is always allowed and needs no
 *   entry here, and neither does `baseUrl`'s own host when that URL is already `http://` — the app
 *   declared that origin in its own source and repeating it here would buy nothing. Everything a
 *   *payload* can name does need an entry, which is what keeps a development convenience out of
 *   release builds:
 *
 *   ```kotlin
 *   // Android emulator reaching the host machine, debug builds only.
 *   allowCleartextHosts = if (BuildConfig.DEBUG) setOf("10.0.2.2") else emptySet()
 *   ```
 *
 *   The platform must agree separately — Android wants `android:usesCleartextTraffic="true"` (or a
 *   network security config) and iOS an ATS exception, and no SDK setting can substitute for
 *   either. Empty in production: cleartext exposes both the payload and the `Authorization`
 *   header to anyone on the path.
 * @property customHttpClient Replaces the SDK's Ktor client entirely. Use it for certificate
 *   pinning or a shared client. Note that doing so **discards** the default timeouts, retry
 *   policy and connection settings; you are responsible for configuring equivalents.
 * @property verifySignatures Refuses any screen whose signature does not verify — fetched, read
 *   back from the cache, or returned by a form submission. Setting [trustedSigningKeys] turns this
 *   on by itself, so a key list can never ship with verification quietly off; set it explicitly
 *   only for the legacy [publicKey] or a [customSignatureVerifier]. Off otherwise, because
 *   verifying against a server that does not sign would reject every screen.
 * @property publicKey **Legacy.** A shared HMAC-SHA256 secret, compared with a hex digest in
 *   `X-Heim-Signature`. The name is historical — it is not a public key. It has to be on the
 *   device to be checked there, so anyone who extracts it from the app can sign screens every
 *   installation accepts. Use [trustedSigningKeys]; the two cannot be combined.
 * @property customSignatureVerifier Replaces verification entirely — a hardware-backed key store,
 *   or a scheme of your own. Takes precedence over [trustedSigningKeys] and [publicKey].
 * @property emergencyBundleProvider Screens bundled with the app, served when the network fails
 *   and no cache exists. The last line of defence before the user sees an error.
 * @property customCacheDataSource Replaces the default in-memory cache.
 *
 *   - `DriverBackedHeimCacheDataSource(driver)` — disk-backed, so screens survive process death
 *     and the app opens instantly offline.
 *   - `NoHeimCacheDataSource()` — disables caching entirely: every open hits the network. Costs
 *     you stale-while-revalidate, offline, and ETag savings, so use it only for content that
 *     must never be shown a moment out of date.
 *
 *   Left null, screens are cached in memory for the life of the process.
 * @property trustedSigningKeys P-256 public keys, as PEM, whose ES256 signatures this app accepts:
 *   the Studio's for what it serves and writes to a bucket, your backend's for what it hydrates,
 *   and both halves of a key rotation while one is under way. Public by design — extracting one
 *   from the app gains nothing, because it cannot sign. A malformed entry fails
 *   [io.heimui.core.HeimUI.initialize] instead of every screen. See [Es256SignatureVerifier].
 *
 *   ```kotlin
 *   trustedSigningKeys = setOf(BuildConfig.HEIMUI_STUDIO_KEY, BuildConfig.HEIMUI_BACKEND_KEY)
 *   ```
 * @property publicScreenHosts Hosts screens may be read from with no credentials at all: a public
 *   bucket or a CDN holding screens that need no data, such as a login or a terms page. A screen
 *   URL on one of them is allowed although it is not `baseUrl`'s origin, and its request never
 *   carries an `Authorization` header — [authTokenProvider] is not even asked. Still `https` only
 *   unless the host is in [allowCleartextHosts], and still verified when signatures are on.
 *
 *   ```kotlin
 *   publicScreenHosts = setOf("screens.example-cdn.com")
 *   // then: HeimScreen(screenId = "https://screens.example-cdn.com/public/@release/login.json")
 *   ```
 *
 * @see io.heimui.core.HeimUI.initialize
 */
public data class HeimConfig(
    val baseUrl: String,
    val authTokenProvider: HeimAuthTokenProvider? = null,
    val allowedSubmitHosts: Set<String> = emptySet(),
    val allowCleartextHosts: Set<String> = emptySet(),
    val customHttpClient: HttpClient? = null,
    val verifySignatures: Boolean = false,
    val publicKey: String? = null,
    val customSignatureVerifier: HeimSignatureVerifier? = null,
    val emergencyBundleProvider: HeimEmergencyBundleProvider? = null,
    val customCacheDataSource: HeimCacheDataSource? = null,
    val trustedSigningKeys: Set<String> = emptySet(),
    val publicScreenHosts: Set<String> = emptySet(),
)

/**
 * Every host this configuration tolerates over cleartext `http://`.
 *
 * [HeimConfig.allowCleartextHosts] plus `baseUrl`'s own host when the base is already cleartext.
 * A `baseUrl` is chosen by the app in its own source, never by a payload, so it is a declaration
 * already; the allowlist exists for the hosts a *payload* can name — an absolute screen URL, a
 * form endpoint, an image — which is where an unchecked `http` would be someone else's decision.
 */
internal fun HeimConfig.cleartextHosts(): Set<String> {
    val baseHost = runCatching { Url(baseUrl) }
        .getOrNull()
        ?.takeIf { it.protocol == URLProtocol.HTTP }
        ?.host
        ?: return allowCleartextHosts
    return allowCleartextHosts + baseHost
}

/** Whether screens are verified: asked for explicitly, or implied by trusting a signing key. */
internal fun HeimConfig.verifiesSignatures(): Boolean =
    verifySignatures || trustedSigningKeys.isNotEmpty()

/** The verifier this configuration asks for, most specific first. */
internal fun HeimConfig.signatureVerifier(): HeimSignatureVerifier =
    customSignatureVerifier
        ?: trustedSigningKeys.takeIf { it.isNotEmpty() }?.let { Es256SignatureVerifier(it) }
        ?: DefaultHeimSignatureVerifier()

/**
 * Refuses a signature configuration that cannot do what it appears to.
 *
 * Checked when the SDK starts rather than when the first screen arrives: a verifier with nothing
 * to verify against rejects every screen, and the first person to find out would be a user looking
 * at an error.
 *
 * @throws IllegalArgumentException naming what is wrong.
 */
internal fun HeimConfig.requireCoherentSignatureSettings() {
    require(trustedSigningKeys.isEmpty() || publicKey.isNullOrBlank()) {
        "Set trustedSigningKeys or the legacy publicKey, not both. publicKey is an HMAC secret and " +
            "would be silently ignored next to them."
    }
    require(
        !verifySignatures || customSignatureVerifier != null || trustedSigningKeys.isNotEmpty() ||
            !publicKey.isNullOrBlank()
    ) {
        "verifySignatures is on but there is nothing to verify against, so every screen would be " +
            "rejected. Add the signers' public keys to trustedSigningKeys."
    }
    if (customSignatureVerifier == null && trustedSigningKeys.isNotEmpty()) {
        // Parsing is the validation: a key that is not a P-256 public key throws, naming its position.
        Es256SignatureVerifier(trustedSigningKeys)
    }
}

/**
 * Builds the Koin module wiring HeimUI's data layer from [config].
 *
 * Exposed for hosts that manage their own Koin graph and want to register HeimUI's dependencies
 * alongside their own. Most apps never call this: [io.heimui.core.HeimUI.initialize] creates an
 * isolated container instead, so the SDK cannot collide with the host's DI setup.
 *
 * @return a Koin [Module] providing [HeimScreenRepository] and its collaborators.
 * @throws IllegalArgumentException if the signature settings in [config] cannot work.
 */
public fun createHeimCoreModule(config: HeimConfig): Module {
    config.requireCoherentSignatureSettings()
    return module {
        single<HeimConfig> { config }

        single<HttpClient> {
            config.customHttpClient ?: HeimRemoteDataSource.createDefaultHttpClient()
        }

        single<HeimSignatureVerifier> { config.signatureVerifier() }

        single<HeimRemoteDataSource> {
            HeimRemoteDataSource(
                httpClient = get(),
                baseUrl = config.baseUrl,
                authTokenProvider = config.authTokenProvider,
                allowedSubmitHosts = config.allowedSubmitHosts,
                allowCleartextHosts = config.cleartextHosts(),
                publicScreenHosts = config.publicScreenHosts,
            )
        }

        single<HeimCacheDataSource> {
            config.customCacheDataSource ?: InMemoryHeimCacheDataSource()
        }

        single<HeimScreenRepository> {
            HeimScreenRepositoryImpl(
                remoteDataSource = get(),
                cacheDataSource = get(),
                signatureVerifier = get(),
                emergencyBundleProvider = config.emergencyBundleProvider,
                verifySignatures = config.verifiesSignatures(),
                publicKey = config.publicKey
            )
        }
    }
}
