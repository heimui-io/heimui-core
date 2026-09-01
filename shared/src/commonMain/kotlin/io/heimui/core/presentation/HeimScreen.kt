package io.heimui.core.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.heimui.core.HeimUI
import io.heimui.core.domain.model.action.DismissModalAction
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.action.OpenUrlAction
import io.heimui.core.domain.model.action.ShowBottomSheetAction
import io.heimui.core.domain.model.action.ShowDialogAction
import io.heimui.core.domain.model.action.SubmitFormAction
import io.heimui.core.domain.model.component.CustomComponent
import io.heimui.core.domain.repository.HeimScreenRepository
import io.heimui.core.presentation.action.LocalHeimActionDispatcher
import io.heimui.core.presentation.component.HeimSkeletonRenderer
import io.heimui.core.presentation.designsystem.LocalHeimIconProvider
import io.heimui.core.presentation.launcher.LocalHeimUrlLauncher
import io.heimui.core.presentation.modal.LocalHeimModalPresenter
import io.heimui.core.presentation.state.HeimScreenState
import io.heimui.core.presentation.state.HeimStateManager
import io.heimui.core.presentation.state.rememberHeimScreenController
import io.heimui.core.presentation.state.rememberHeimStateManager
import io.heimui.core.presentation.telemetry.HeimTelemetryEvent
import io.heimui.core.presentation.telemetry.LocalHeimTelemetryObserver
import io.heimui.core.presentation.validation.LocalHeimValidatorRegistry
import kotlinx.coroutines.launch

/**
 * Renders a server-driven screen.
 *
 * Fetches the screen identified by [screenId], renders its component tree natively, and routes
 * the actions it declares. Loading, error and stale states are handled internally.
 *
 * ```kotlin
 * HeimTheme {
 *     HeimScreen(
 *         screenId = "checkout",
 *         onAction = { action ->
 *             when (action) {
 *                 is NavigateAction -> navController.navigate(action.screenId)
 *                 else -> Unit
 *             }
 *         },
 *     )
 * }
 * ```
 *
 * ### Which actions the SDK handles for you
 * `ShowBottomSheetAction`, `ShowDialogAction`, `DismissModalAction`, `OpenUrlAction` and
 * `SubmitFormAction` are executed internally — form submission includes validating every
 * registered field and interpolating `{{state.key}}` placeholders before the request is sent.
 * **Every** action is still forwarded to [onAction] afterwards, so you can observe them all;
 * navigation is the one you are expected to implement, since only the host knows its own
 * navigation graph.
 *
 * ### Requirements
 * Must be called inside [io.heimui.core.presentation.designsystem.HeimTheme], which installs the
 * image loader, modal presenter, telemetry observer and URL policy this composable reads.
 *
 * @param screenId identifier resolved against `HeimConfig.baseUrl` as `/screens/{screenId}`.
 * @param onAction invoked for every action the payload dispatches, after the SDK has handled it.
 * @param modifier applied to the root container.
 * @param repository overrides the shared [HeimUI.repository]. Pass one explicitly in previews and
 *   tests; leaving it `null` requires [HeimUI.initialize] to have run.
 * @param queryParams appended to the fetch request. Changing this reloads the screen.
 * @param enablePullToRefresh whether rendered content responds to pull-to-refresh.
 * @param stateManager form state holder. The default survives configuration changes, and process
 *   death when a draft storage is installed on the theme.
 * @param customRenderer fallback for `CustomComponent`s with no entry in the component registry.
 *   The registry takes precedence, since it matches by component name.
 *
 * @see HeimTheme
 * @see io.heimui.core.presentation.registry.HeimCustomComponentRegistry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun HeimScreen(
    screenId: String,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier,
    repository: HeimScreenRepository? = null,
    queryParams: Map<String, String> = emptyMap(),
    enablePullToRefresh: Boolean = true,
    stateManager: HeimStateManager = rememberHeimStateManager(screenId = screenId),
    customRenderer: (@Composable (CustomComponent) -> Unit)? = null
) {
    val activeRepository = repository ?: remember { runCatching { HeimUI.repository }.getOrNull() }

    if (activeRepository == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "HeimUI is not initialized. Call HeimUI.initialize() or pass a repository.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val actionDispatcher = LocalHeimActionDispatcher.current
    val urlLauncher = LocalHeimUrlLauncher.current
    val telemetryObserver = LocalHeimTelemetryObserver.current
    val modalPresenter = LocalHeimModalPresenter.current
    val validatorRegistry = LocalHeimValidatorRegistry.current
    val coroutineScope = rememberCoroutineScope()

    // Loading, refresh and modal state live in the controller, so they survive configuration
    // changes and are reachable from unit tests without a Compose UI harness.
    val controller = rememberHeimScreenController(
        screenId = screenId,
        repository = activeRepository,
        queryParams = queryParams,
        telemetryObserver = telemetryObserver
    )
    val screenState by controller.screenState
    val isRefreshing by controller.isRefreshing
    val activeBottomSheet by controller.activeBottomSheet
    val activeDialog by controller.activeDialog

    // `queryParams` is a key: changing the query used to leave the screen showing results for
    // the previous parameters.
    LaunchedEffect(screenId, queryParams, activeRepository) {
        controller.load(resetToLoading = true)
    }

    val currentOnAction by rememberUpdatedState(onAction)

    // `screenId` is keyed explicitly. The lambda closes over it for telemetry, and relying on
    // `stateManager` changing alongside it was an indirect dependency waiting to break.
    val handleAction: (HeimAction) -> Unit = remember(
        screenId, actionDispatcher, urlLauncher, telemetryObserver,
        stateManager, validatorRegistry, controller
    ) {
        { action ->
            coroutineScope.launch {
                actionDispatcher.dispatch(action, stateManager) { dispatched ->
                    when (dispatched) {
                        is ShowBottomSheetAction -> controller.showBottomSheet(dispatched)
                        is ShowDialogAction -> controller.showDialog(dispatched)
                        is DismissModalAction -> controller.dismissModals()
                        is OpenUrlAction -> urlLauncher.openUrl(dispatched.url)
                        is SubmitFormAction -> coroutineScope.launch {
                            controller.submitForm(
                                action = dispatched,
                                stateManager = stateManager,
                                validatorRegistry = validatorRegistry
                            )
                        }
                        else -> Unit
                    }
                    telemetryObserver.onEvent(
                        HeimTelemetryEvent.ActionExecuted(
                            screenId = screenId,
                            actionType = dispatched.telemetryName
                        )
                    )
                    currentOnAction(dispatched)
                }
            }
        }
    }

    val content: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = screenState) {
                is HeimScreenState.Loading -> HeimSkeletonRenderer()

                is HeimScreenState.Content -> HeimScreenRenderer(
                    response = state.screen,
                    stateManager = stateManager,
                    onAction = handleAction,
                    customRenderer = customRenderer,
                    modifier = Modifier.fillMaxSize()
                )

                is HeimScreenState.Error -> HeimErrorView(
                    message = state.message,
                    onRetry = { controller.retry() },
                    modifier = Modifier.align(Alignment.Center)
                )

                // Reachable now: a payload whose root renders nothing resolves to Empty instead
                // of showing a blank screen. This branch used to be dead code.
                is HeimScreenState.Empty -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("heim_screen_$screenId")) {
        // Pull-to-refresh wraps rendered content only. Wrapping Loading and Error left the
        // gesture live over a skeleton, and the box kept consuming nested scroll even when the
        // feature was disabled.
        if (enablePullToRefresh && screenState is HeimScreenState.Content) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { controller.refresh() },
                modifier = Modifier.fillMaxSize()
            ) { content() }
        } else {
            content()
        }

        activeBottomSheet?.let { sheetAction ->
            modalPresenter.RenderBottomSheet(
                action = sheetAction,
                onDismiss = { controller.dismissBottomSheet() },
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

        activeDialog?.let { dialogAction ->
            modalPresenter.RenderDialog(
                action = dialogAction,
                onDismiss = { controller.dismissDialog() },
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
            .padding(24.dp)
            .testTag("heim_error_view"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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
                ),
                modifier = Modifier.testTag("heim_error_retry")
            ) {
                Text(text = "Retry")
            }
        }
    }
}
