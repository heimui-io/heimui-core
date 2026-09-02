package io.heimui.core.data.repository

import io.heimui.core.data.datasource.local.HeimCacheDataSource
import io.heimui.core.data.datasource.local.HeimEmergencyBundleProvider
import io.heimui.core.data.datasource.remote.HeimRemoteDataSource
import io.heimui.core.data.datasource.remote.RemoteScreenResponse
import io.heimui.core.data.datasource.remote.RemoteSubmitResponse
import io.heimui.core.data.mapper.toDomain
import io.heimui.core.data.security.DefaultHeimSignatureVerifier
import io.heimui.core.data.security.HeimSignatureVerifier
import io.heimui.core.domain.model.HeimValue
import io.heimui.core.domain.repository.HeimScreenRepository
import io.heimui.core.domain.repository.HeimScreenResult
import io.heimui.core.domain.repository.HeimSubmitResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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

        when (val remote = remoteDataSource.fetchScreen(screenId, queryParams, trustedCache?.etag)) {
            is RemoteScreenResponse.Success -> {
                if (verifySignatures &&
                    !signatureVerifier.verify(remote.rawBytes, remote.signature, publicKey)
                ) {
                    emit(HeimScreenResult.Error("Security verification failed: payload signature invalid"))
                    return@flow
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

            is RemoteScreenResponse.CircuitOpen -> emitDegraded(screenId, trustedCache, remote.message)
            is RemoteScreenResponse.Error -> emitDegraded(screenId, trustedCache, remote.message)
        }
    }.flowOn(workDispatcher)

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
        return signatureVerifier.verify(rawBytes, signature, publicKey)
    }

    override suspend fun submitForm(
        endpoint: String,
        method: String,
        payload: Map<String, HeimValue>?
    ): HeimSubmitResult {
        return when (val response = remoteDataSource.submitForm(endpoint, method, payload)) {
            is RemoteSubmitResponse.Success -> HeimSubmitResult.Success(
                responseScreen = response.responseScreen?.toDomain(),
                message = "Form submitted successfully"
            )
            is RemoteSubmitResponse.Error -> HeimSubmitResult.Error(response.message)
            is RemoteSubmitResponse.SecurityViolation -> HeimSubmitResult.Blocked(response.message)
        }
    }
}
