package io.heimui.core.domain.repository

import io.heimui.core.domain.model.HeimScreenResponse

interface HeimScreenRepository {
    suspend fun getScreen(screenId: String, queryParams: Map<String, String> = emptyMap()): Result<HeimScreenResponse>
    suspend fun cacheScreen(screen: HeimScreenResponse)
    suspend fun getCachedScreen(screenId: String): HeimScreenResponse?
}
