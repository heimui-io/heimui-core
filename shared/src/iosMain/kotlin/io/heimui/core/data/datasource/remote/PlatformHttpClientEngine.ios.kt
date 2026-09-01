package io.heimui.core.data.datasource.remote

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*

public actual fun createPlatformHttpClientEngine(): HttpClientEngine = Darwin.create {
    configureRequest {
        setAllowsCellularAccess(true)
    }
}
