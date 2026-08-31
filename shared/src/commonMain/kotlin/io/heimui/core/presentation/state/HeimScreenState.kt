package io.heimui.core.presentation.state

import io.heimui.core.domain.model.HeimScreenResponse

sealed interface HeimScreenState {
    data object Loading : HeimScreenState
    data class Content(val screen: HeimScreenResponse, val isStale: Boolean = false) : HeimScreenState
    data class Error(val message: String, val onRetry: () -> Unit) : HeimScreenState
    data object Empty : HeimScreenState
}
