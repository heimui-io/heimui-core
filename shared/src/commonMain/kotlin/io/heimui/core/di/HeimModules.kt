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

data class HeimConfig(
    val baseUrl: String,
    val authTokenProvider: (() -> String?)? = null,
    val customHttpClient: HttpClient? = null,
    val verifySignatures: Boolean = false,
    val publicKey: String? = null,
    val customSignatureVerifier: HeimSignatureVerifier? = null,
    val emergencyBundleProvider: HeimEmergencyBundleProvider? = null,
    val customCacheDataSource: HeimCacheDataSource? = null
)

fun createHeimCoreModule(config: HeimConfig): Module = module {
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
            authTokenProvider = config.authTokenProvider
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
