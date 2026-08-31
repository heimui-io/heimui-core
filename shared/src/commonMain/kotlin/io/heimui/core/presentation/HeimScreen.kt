package io.heimui.core.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.component.CustomComponent
import io.heimui.core.domain.repository.HeimScreenRepository
import io.heimui.core.domain.repository.HeimScreenResult
import io.heimui.core.presentation.component.HeimSkeletonRenderer
import io.heimui.core.presentation.designsystem.LocalHeimIconProvider
import io.heimui.core.presentation.state.HeimScreenState
import io.heimui.core.presentation.state.HeimStateManager
import io.heimui.core.presentation.telemetry.HeimTelemetryEvent
import io.heimui.core.presentation.telemetry.LocalHeimTelemetryObserver
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HeimScreen(
    screenId: String,
    repository: HeimScreenRepository,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier,
    queryParams: Map<String, String> = emptyMap(),
    stateManager: HeimStateManager = remember(screenId) { HeimStateManager(screenId = screenId) },
    customRenderer: (@Composable (CustomComponent) -> Unit)? = null
) {
    var screenState by remember(screenId) { mutableStateOf<HeimScreenState>(HeimScreenState.Loading) }
    var retryCount by remember { mutableIntStateOf(0) }
    var activeBottomSheet by remember { mutableStateOf<io.heimui.core.domain.model.action.ShowBottomSheetAction?>(null) }
    var activeDialog by remember { mutableStateOf<io.heimui.core.domain.model.action.ShowDialogAction?>(null) }
    val telemetryObserver = LocalHeimTelemetryObserver.current

    LaunchedEffect(screenId, retryCount) {
        screenState = HeimScreenState.Loading
        repository.getScreen(screenId, queryParams).collectLatest { result ->
            when (result) {
                is HeimScreenResult.Success -> {
                    screenState = HeimScreenState.Content(
                        screen = result.screen,
                        isStale = result.isStale
                    )
                    telemetryObserver.onEvent(
                        HeimTelemetryEvent.ScreenViewed(
                            screenId = screenId,
                            isStale = result.isStale
                        )
                    )
                }
                is HeimScreenResult.Error -> {
                    screenState = HeimScreenState.Error(
                        message = result.message,
                        onRetry = { retryCount++ }
                    )
                    telemetryObserver.onEvent(
                        HeimTelemetryEvent.ScreenError(
                            screenId = screenId,
                            errorMessage = result.message,
                            throwable = result.throwable
                        )
                    )
                }
            }
        }
    }

    val handleAction: (HeimAction) -> Unit = { action ->
        when (action) {
            is io.heimui.core.domain.model.action.ShowBottomSheetAction -> activeBottomSheet = action
            is io.heimui.core.domain.model.action.ShowDialogAction -> activeDialog = action
            is io.heimui.core.domain.model.action.DismissModalAction -> {
                activeBottomSheet = null
                activeDialog = null
            }
            else -> {}
        }
        telemetryObserver.onEvent(
            HeimTelemetryEvent.ActionExecuted(
                actionType = action::class.simpleName ?: "UnknownAction"
            )
        )
        onAction(action)
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = screenState) {
            is HeimScreenState.Loading -> {
                HeimSkeletonRenderer()
            }
            is HeimScreenState.Content -> {
                HeimScreenRenderer(
                    response = state.screen,
                    stateManager = stateManager,
                    onAction = handleAction,
                    customRenderer = customRenderer,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is HeimScreenState.Error -> {
                HeimErrorView(
                    message = state.message,
                    onRetry = state.onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is HeimScreenState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No content available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val modalPresenter = io.heimui.core.presentation.modal.LocalHeimModalPresenter.current

        // 1. Dynamic Bottom Sheet
        activeBottomSheet?.let { sheetAction ->
            modalPresenter.RenderBottomSheet(
                action = sheetAction,
                onDismiss = { activeBottomSheet = null },
                onAction = handleAction
            ) {
                HeimRenderer(
                    component = sheetAction.content,
                    stateManager = stateManager,
                    onAction = handleAction,
                    customRenderer = customRenderer
                )
            }
        }

        // 2. Dynamic Alert Dialog
        activeDialog?.let { dialogAction ->
            modalPresenter.RenderDialog(
                action = dialogAction,
                onDismiss = { activeDialog = null },
                onAction = handleAction
            )
        }
    }
}

@Composable
private fun HeimErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LocalHeimIconProvider.current.RenderIcon(
                name = "info",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                size = 36.dp,
                modifier = Modifier
            )
            Text(
                text = "Unable to load screen",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(text = "Retry")
            }
        }
    }
}
