package io.heimui.core.data.datasource.remote

import io.ktor.client.engine.*

expect fun createPlatformHttpClientEngine(): HttpClientEngine
