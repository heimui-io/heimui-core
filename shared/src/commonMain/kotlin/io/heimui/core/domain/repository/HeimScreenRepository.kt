package io.heimui.core.domain.repository

import io.heimui.core.domain.model.HeimScreenResponse
import kotlinx.coroutines.flow.Flow

sealed interface HeimScreenResult {
    data class Success(val screen: HeimScreenResponse, val isStale: Boolean = false) : HeimScreenResult
    data class Error(val message: String, val throwable: Throwable? = null) : HeimScreenResult
}

sealed interface HeimSubmitResult {
    data class Success(val responseScreen: HeimScreenResponse? = null, val message: String? = null) : HeimSubmitResult
    data class Error(val message: String, val throwable: Throwable? = null) : HeimSubmitResult
}

interface HeimScreenRepository {
    fun getScreen(screenId: String, queryParams: Map<String, String> = emptyMap()): Flow<HeimScreenResult>
    suspend fun submitForm(endpoint: String, method: String = "POST", payload: Map<String, Any?>? = null): HeimSubmitResult
}
