package io.heimui.core.data.datasource.remote

import io.ktor.client.engine.*

internal expect fun createPlatformHttpClientEngine(): HttpClientEngine
