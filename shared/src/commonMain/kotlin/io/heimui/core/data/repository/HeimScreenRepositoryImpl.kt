package io.heimui.core.data.repository

import io.heimui.core.data.datasource.local.HeimCacheDataSource
import io.heimui.core.data.datasource.local.HeimEmergencyBundleProvider
import io.heimui.core.data.datasource.remote.HeimRemoteDataSource
import io.heimui.core.data.datasource.remote.RemoteScreenResponse
import io.heimui.core.data.datasource.remote.RemoteSubmitResponse
import io.heimui.core.data.mapper.toDomain
import io.heimui.core.data.security.DefaultHeimSignatureVerifier
import io.heimui.core.data.security.Es256SignatureVerifier
import io.heimui.core.data.security.HeimSignatureVerifier
import io.heimui.core.domain.model.HeimValue
import io.heimui.core.domain.repository.HeimScreenRepository
import io.heimui.core.domain.repository.HeimScreenResult
import io.heimui.core.domain.repository.HeimSubmitResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal class HeimScreenRepositoryImpl(
    private val remoteDataSource: HeimRemoteDataSource,
    private val cacheDataSource: HeimCacheDataSource,
    private val signatureVerifier: HeimSignatureVerifier = DefaultHeimSignatureVerifier(),
    private val emergencyBundleProvider: HeimEmergencyBundleProvider? = null,
    private val verifySignatures: Boolean = false,
    private val publicKey: String? = null,
    /**
     * JSON parsing and the recursive DTO -> domain mapping run here. Without this the whole
     * pipeline executes on the collector's context, which is the main thread.
     */
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default
) : HeimScreenRepository {

    override fun getScreen(
        screenId: String,
        queryParams: Map<String, String>
    ): Flow<HeimScreenResult> = flow {
        // Keyed by the URL actually fetched, not by the screen id. Deriving it from the data
        // source is what keeps the two from drifting apart.
        val cacheKey = remoteDataSource.screenCacheKey(screenId, queryParams)
        val cachedEntry = cacheDataSource.getScreen(cacheKey)

        // Cached content is only trusted if it still carries a signature we can re-verify.
        // A cache is attacker-writable on a rooted/jailbroken device, so rendering it with
        // implicit trust would reopen the very hole signature verification is there to close.
        val trustedCache = when {
            cachedEntry == null -> null
            !verifySignatures -> cachedEntry
            isCachedEntryTrusted(cachedEntry.signature, cachedEntry.rawBytes) -> cachedEntry
            else -> {
                cacheDataSource.clear(cacheKey)
                null
            }
        }

        if (trustedCache != null) {
            emit(HeimScreenResult.Success(screen = trustedCache.screen.toDomain(), isStale = true))
        }

        // Inside the flow, not in the `catch` below, because only here is the cache still in scope.
        // A payload that will not parse is the server failing -- the same event as a 500, and it
        // gets the same answer: whatever the device can already read stays on screen. Handled out
        // there instead, a bad deploy replaced a working screen with an error card for everyone
        // holding a perfectly good copy of it.
        try {
        when (val remote = remoteDataSource.fetchScreen(screenId, queryParams, trustedCache?.etag)) {
            is RemoteScreenResponse.Success -> {
                if (verifySignatures) {
                    val failure = verificationFailure(remote.rawBytes, remote.signature)
                    if (failure != null) {
                        emit(HeimScreenResult.Error("Security verification failed: $failure"))
                        return@flow
                    }
                }
                cacheDataSource.saveScreen(
                    screenId = cacheKey,
                    screen = remote.screen,
                    etag = remote.etag,
                    signature = remote.signature,
                    rawBytes = remote.rawBytes
                )
                emit(HeimScreenResult.Success(screen = remote.screen.toDomain(), isStale = false))
            }

            is RemoteScreenResponse.NotModified -> {
                emit(
                    trustedCache
                        ?.let { HeimScreenResult.Success(it.screen.toDomain(), isStale = false) }
                        ?: HeimScreenResult.Error(
                            "Server reported 304 Not Modified but no valid local cache exists."
                        )
                )
            }

            is RemoteScreenResponse.ErrorScreen -> {
                // Verified exactly like a 200. A 4xx is the one response an intermediary can most
                // easily put in front of an app -- a captive portal, a proxy, a WAF -- so relaxing
                // this here would undo the point of signing at all.
                if (verifySignatures) {
                    val failure = verificationFailure(remote.rawBytes, remote.signature)
                    if (failure != null) {
                        emitDegraded(screenId, trustedCache, "Security verification failed: $failure")
                        return@flow
                    }
                }
                // Not written to the cache, and the entry already there is left untouched. What the
                // server refused today says nothing about what that screen contains tomorrow.
                emit(HeimScreenResult.Refused(remote.screen.toDomain(), remote.statusCode))
            }

            is RemoteScreenResponse.CircuitOpen -> emitDegraded(screenId, trustedCache, remote.message)
            is RemoteScreenResponse.Error -> emitDegraded(screenId, trustedCache, remote.message)
        }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            emitDegraded(screenId, trustedCache, cause.message ?: "Failed to load screen")
        }
    }
        // The collector in HeimScreenController has no catch of its own, so anything escaping
        // this flow takes the host app's process down with it. A screen that cannot be loaded is
        // an error state the UI already knows how to render -- never a crash.
        .catch { cause ->
            if (cause is CancellationException) throw cause
            emit(HeimScreenResult.Error(cause.message ?: "Failed to load screen"))
        }
        .flowOn(workDispatcher)

    /**
     * Terminal emission when the network could not deliver fresh content.
     *
     * Order of preference: keep the cache visible, then an emergency bundle, then a hard error.
     * Never emits [HeimScreenResult.Error] while usable content exists.
     */
    private suspend fun kotlinx.coroutines.flow.FlowCollector<HeimScreenResult>.emitDegraded(
        screenId: String,
        trustedCache: io.heimui.core.data.datasource.local.CachedScreenEntry?,
        reason: String
    ) {
        if (trustedCache != null) {
            emit(HeimScreenResult.Stale(trustedCache.screen.toDomain(), reason = reason))
            return
        }
        val emergency = emergencyBundleProvider?.getEmergencyScreen(screenId)
        if (emergency != null) {
            emit(
                HeimScreenResult.Stale(
                    emergency.toDomain(),
                    reason = "$reason (serving bundled emergency screen)"
                )
            )
            return
        }
        emit(HeimScreenResult.Error(reason))
    }

    private fun isCachedEntryTrusted(signature: String?, rawBytes: ByteArray?): Boolean {
        if (signature == null || rawBytes == null) return false
        return verificationFailure(rawBytes, signature) == null
    }

    /**
     * Why a screen fails verification, or null when it passes.
     *
     * The ES256 verifier can say which of its checks failed — an unsigned screen, a key this app
     * does not trust, content that changed after signing — and that sentence is the difference
     * between fixing a key in a minute and reading this source. Any other verifier only answers
     * yes or no, and keeps the message it always had.
     */
    private fun verificationFailure(bytes: ByteArray, signature: String?): String? {
        val verifier = signatureVerifier
        if (verifier is Es256SignatureVerifier) return verifier.failureReason(bytes, signature)
        return if (verifier.verify(bytes, signature, publicKey)) null else "payload signature invalid"
    }

    override suspend fun submitForm(
        endpoint: String,
        method: String,
        payload: Map<String, HeimValue>?
    ): HeimSubmitResult {
        return when (val response = remoteDataSource.submitForm(endpoint, method, payload)) {
            is RemoteSubmitResponse.Success -> {
                val screen = response.responseScreen
                // A screen returned by a submission is rendered exactly like a fetched one, so it is
                // held to the same signature. Unchecked, it would be the way around verification:
                // any endpoint a form can post to could answer with a screen of its own choosing.
                val failure = if (screen != null && verifySignatures) {
                    verificationFailure(response.rawBytes ?: ByteArray(0), response.signature)
                } else {
                    null
                }
                if (failure != null) {
                    HeimSubmitResult.Error(
                        "Security verification failed for the screen the submission returned: $failure"
                    )
                } else {
                    HeimSubmitResult.Success(
                        responseScreen = screen?.toDomain(),
                        message = "Form submitted successfully"
                    )
                }
            }
            is RemoteSubmitResponse.Error -> HeimSubmitResult.Error(response.message)
            is RemoteSubmitResponse.SecurityViolation -> HeimSubmitResult.Blocked(response.message)
        }
    }
}
