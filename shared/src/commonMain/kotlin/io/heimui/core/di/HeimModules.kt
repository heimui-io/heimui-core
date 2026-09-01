package io.heimui.core.di

import io.heimui.core.data.datasource.local.HeimCacheDataSource
import io.heimui.core.data.datasource.local.HeimEmergencyBundleProvider
import io.heimui.core.data.datasource.local.InMemoryHeimCacheDataSource
import io.heimui.core.data.datasource.remote.HeimRemoteDataSource
import io.heimui.core.data.repository.HeimScreenRepositoryImpl
import io.heimui.core.data.security.DefaultHeimSignatureVerifier
import io.heimui.core.data.security.HeimSignatureVerifier
import io.heimui.core.domain.repository.HeimScreenRepository
import io.ktor.client.HttpClient
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
 * @property baseUrl Origin the SDK fetches screens from. Screens resolve to `$baseUrl/screens/{id}`.
 *   Also defines the trust boundary for form submissions: a payload cannot submit to another host
 *   unless it is listed in [allowedSubmitHosts].
 * @property authTokenProvider Supplies the `Authorization` header value, including its scheme
 *   (`"Bearer eyJ..."`). Invoked per request, so a rotating token needs no reconfiguration.
 *   Return `null` to send the request unauthenticated.
 * @property allowedSubmitHosts Extra hosts permitted to receive authenticated form submissions.
 *   Empty by default: only [baseUrl]'s own origin is allowed, which is what prevents a malicious
 *   payload from exfiltrating the session token to a third party.
 * @property customHttpClient Replaces the SDK's Ktor client entirely. Use it for certificate
 *   pinning or a shared client. Note that doing so **discards** the default timeouts, retry policy
 *   and connection settings; you are responsible for configuring equivalents.
 * @property verifySignatures Enables cryptographic verification of screen payloads. Requires
 *   [publicKey] and a server that signs responses with the `X-Heim-Signature` header. Off by
 *   default because verification against an unsigned backend would reject every screen.
 * @property publicKey Key material handed to the verifier. Its meaning depends on the
 *   implementation: a shared secret for the default HMAC verifier, a public key for an asymmetric
 *   one.
 * @property customSignatureVerifier Plugs in your own verification, typically to use a
 *   hardware-backed key store or an asymmetric algorithm.
 * @property emergencyBundleProvider Screens bundled with the app, served when the network fails
 *   and no cache exists. The last line of defence before the user sees an error.
 * @property customCacheDataSource Replaces the in-memory cache. Supply a disk-backed
 *   implementation to make screens survive process death.
 *
 * @see io.heimui.core.HeimUI.initialize
 */
public data class HeimConfig(
    val baseUrl: String,
    val authTokenProvider: (() -> String?)? = null,
    val allowedSubmitHosts: Set<String> = emptySet(),
    val customHttpClient: HttpClient? = null,
    val verifySignatures: Boolean = false,
    val publicKey: String? = null,
    val customSignatureVerifier: HeimSignatureVerifier? = null,
    val emergencyBundleProvider: HeimEmergencyBundleProvider? = null,
    val customCacheDataSource: HeimCacheDataSource? = null
)

/**
 * Builds the Koin module wiring HeimUI's data layer from [config].
 *
 * Exposed for hosts that manage their own Koin graph and want to register HeimUI's dependencies
 * alongside their own. Most apps never call this: [io.heimui.core.HeimUI.initialize] creates an
 * isolated container instead, so the SDK cannot collide with the host's DI setup.
 *
 * @return a Koin [Module] providing [HeimScreenRepository] and its collaborators.
 */
public fun createHeimCoreModule(config: HeimConfig): Module = module {
    single<HeimConfig> { config }

    single<HttpClient> {
        config.customHttpClient ?: HeimRemoteDataSource.createDefaultHttpClient()
    }

    single<HeimSignatureVerifier> {
        config.customSignatureVerifier ?: DefaultHeimSignatureVerifier()
    }

    single<HeimRemoteDataSource> {
        HeimRemoteDataSource(
            httpClient = get(),
            baseUrl = config.baseUrl,
            authTokenProvider = config.authTokenProvider,
            allowedSubmitHosts = config.allowedSubmitHosts
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
            verifySignatures = config.verifySignatures,
            publicKey = config.publicKey
        )
    }
}
