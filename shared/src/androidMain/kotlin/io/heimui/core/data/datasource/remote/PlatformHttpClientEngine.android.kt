package io.heimui.core.data.datasource.remote

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*

internal actual fun createPlatformHttpClientEngine(): HttpClientEngine = OkHttp.create {
    config {
        retryOnConnectionFailure(true)
    }
}
