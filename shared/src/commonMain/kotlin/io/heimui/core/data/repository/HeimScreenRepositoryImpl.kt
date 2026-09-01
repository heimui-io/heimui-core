package io.heimui.core.data.repository

import io.heimui.core.data.datasource.local.HeimCacheDataSource
import io.heimui.core.data.datasource.local.HeimEmergencyBundleProvider
import io.heimui.core.data.datasource.remote.HeimRemoteDataSource
import io.heimui.core.data.datasource.remote.RemoteScreenResponse
import io.heimui.core.data.datasource.remote.RemoteSubmitResponse
import io.heimui.core.data.mapper.toDomain
import io.heimui.core.data.security.DefaultHeimSignatureVerifier
import io.heimui.core.data.security.HeimSignatureVerifier
import io.heimui.core.domain.repository.HeimScreenRepository
import io.heimui.core.domain.repository.HeimScreenResult
import io.heimui.core.domain.repository.HeimSubmitResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class HeimScreenRepositoryImpl(
    private val remoteDataSource: HeimRemoteDataSource,
    private val cacheDataSource: HeimCacheDataSource,
    private val signatureVerifier: HeimSignatureVerifier = DefaultHeimSignatureVerifier(),
    private val emergencyBundleProvider: HeimEmergencyBundleProvider? = null,
    private val verifySignatures: Boolean = false,
    private val publicKey: String? = null
) : HeimScreenRepository {

    override fun getScreen(
        screenId: String,
        queryParams: Map<String, String>
    ): Flow<HeimScreenResult> = flow {
        val cachedEntry = cacheDataSource.getScreen(screenId)
        if (cachedEntry != null) {
            emit(HeimScreenResult.Success(screen = cachedEntry.screen.toDomain(), isStale = true))
        }

        when (val remoteResponse = remoteDataSource.fetchScreen(screenId, queryParams, cachedEntry?.etag)) {
            is RemoteScreenResponse.Success -> {
                if (verifySignatures) {
                    val isValid = signatureVerifier.verify(
                        payload = remoteResponse.screen.id,
                        signature = remoteResponse.screen.signature,
                        publicKey = publicKey
                    )
                    if (!isValid) {
                        emit(HeimScreenResult.Error(message = "Security verification failed: payload signature invalid"))
                        return@flow
                    }
                }
                cacheDataSource.saveScreen(screenId, remoteResponse.screen, remoteResponse.etag)
                emit(HeimScreenResult.Success(screen = remoteResponse.screen.toDomain(), isStale = false))
            }
            is RemoteScreenResponse.NotModified -> {
                if (cachedEntry != null) {
                    emit(HeimScreenResult.Success(screen = cachedEntry.screen.toDomain(), isStale = false))
                }
            }
            is RemoteScreenResponse.Error -> {
                if (cachedEntry == null) {
                    val emergency = emergencyBundleProvider?.getEmergencyScreen(screenId)
                    if (emergency != null) {
                        emit(HeimScreenResult.Success(screen = emergency.toDomain(), isStale = true))
                    } else {
                        emit(HeimScreenResult.Error(message = remoteResponse.message))
                    }
                }
            }
        }
    }

    override suspend fun submitForm(
        endpoint: String,
        method: String,
        payload: Map<String, Any?>?
    ): HeimSubmitResult {
        return when (val response = remoteDataSource.submitForm(endpoint, method, payload)) {
            is RemoteSubmitResponse.Success -> {
                HeimSubmitResult.Success(
                    responseScreen = response.responseScreen?.toDomain(),
                    message = "Form submitted successfully"
                )
            }
            is RemoteSubmitResponse.Error -> {
                HeimSubmitResult.Error(
                    message = response.message
                )
            }
        }
    }
}
