package io.heimui.core.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.CompositionLocalProvider
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
import io.heimui.core.domain.model.action.SetStateAction
import io.heimui.core.presentation.action.HeimActionRunner
import io.heimui.core.presentation.action.LocalHeimActionRunner
import io.heimui.core.presentation.state.HeimStateScope
import io.heimui.core.presentation.action.runHeimActionSequence
import io.heimui.core.presentation.launcher.LocalHeimUrlLauncher
import io.heimui.core.presentation.tracking.LocalHeimTrackingDispatcher
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
 * @param screenId identifier resolved against `HeimConfig.baseUrl` (or an absolute URL).
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
 * @param errorContent replaces the built-in failure card. The default is deliberately plain and
 *   written in English, which is wrong in any app that is not: pass your own to say it in your
 *   product's voice and language. `onRetry` refetches. Receives a [BoxScope], so
 *   `Modifier.align(Alignment.Center)` works -- without it a replacement lands in the top-left
 *   corner while the card it replaced was centred.
 * @param loadingContent replaces the built-in skeleton, for an app with its own loading style.
 * @param overlayScreenId a second screen drawn on top of this one, or `null` for none. This is how
 *   an in-app notification works: it is an ordinary screen, authored and published like the rest,
 *   and the host decides when it appears. It never shows a skeleton or an error of its own -- it
 *   arrives when it is ready or not at all, because neither belongs over a screen that is working.
 * @param onOverlayDismiss called when the overlay dispatches `dismiss_modal`, the same action that
 *   closes a dialog or a bottom sheet. Set your state to `null` here.
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
    customRenderer: (@Composable (CustomComponent) -> Unit)? = null,
    errorContent: (@Composable BoxScope.(message: String, onRetry: () -> Unit) -> Unit)? = null,
    loadingContent: (@Composable BoxScope.() -> Unit)? = null,
    overlayScreenId: String? = null,
    onOverlayDismiss: (() -> Unit)? = null
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

    // The payload carries the version its state keys belong to, and it exists only once the screen
    // has loaded -- after `stateManager` had to be constructed. Handing it over here is what makes
    // the draft-version guard able to fire at all.
    val loadedContent = screenState as? HeimScreenState.Content
    val loadedVersion = loadedContent?.screen?.version
    val loadedIsStale = loadedContent?.isStale == true
    LaunchedEffect(stateManager, loadedVersion, loadedIsStale) {
        loadedVersion?.let { stateManager.applyScreenVersion(it, loadedIsStale) }
    }
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
    val trackingDispatcher = LocalHeimTrackingDispatcher.current

    // Executes one action and reports whether the ones after it may run. Only submission can
    // fail in a way the sequence must respect; everything else either happens or is a no-op.
    val executeAction: suspend (HeimAction, HeimStateScope) -> Boolean = remember(
        screenId, urlLauncher, telemetryObserver, stateManager,
        validatorRegistry, controller, trackingDispatcher
    ) {
        { action, actionScope ->
            var mayContinue = true
            when (action) {
                is ShowBottomSheetAction -> controller.showBottomSheet(action)
                is ShowDialogAction -> controller.showDialog(action)
                is DismissModalAction -> controller.dismissModals()
                is OpenUrlAction -> urlLauncher.openUrl(action.url)
                is SetStateAction -> stateManager.updateValue(
                    // Writing inside the row that raised it, so a "clear" button in one row does
                    // not empty the field in every other row.
                    actionScope.resolve(action.key),
                    // A null writes an empty string rather than removing the key: `visible_if`
                    // distinguishes "empty" from "never set", and a payload clearing a field means
                    // the first.
                    action.value.asString.orEmpty()
                )
                is SubmitFormAction -> {
                    // Awaited, not launched. This is the whole point of the runner: an action
                    // listed after a submission must see whether it succeeded.
                    mayContinue = controller.submitForm(
                        action = action,
                        stateManager = stateManager,
                        validatorRegistry = validatorRegistry,
                        scope = actionScope
                    )
                }
                else -> Unit
            }
            action.tracking?.let { trackingDispatcher.track(action, it) }
            telemetryObserver.onEvent(
                HeimTelemetryEvent.ActionExecuted(
                    screenId = screenId,
                    actionType = action.telemetryName
                )
            )
            currentOnAction(action)
            mayContinue
        }
    }

    // Kept for the single-action call sites and for anything the host dispatches itself.
    val handleAction: (HeimAction) -> Unit = remember(actionDispatcher, executeAction, stateManager) {
        { action ->
            coroutineScope.launch {
                // `dispatch` hands the result to a plain callback, so the action is recorded here
                // and executed after it returns. Executing inside the callback would launch a
                // second coroutine and lose the ordering the caller is relying on.
                var resolved: HeimAction? = null
                actionDispatcher.dispatch(action, stateManager) { resolved = it }
                // Dispatched by the host rather than by a component, so there is no row to be
                // inside: the screen's own namespace is the only sensible reading.
                resolved?.let { executeAction(it, HeimStateScope.Root) }
            }
        }
    }

    // One coroutine for the whole list, so the order the payload wrote is the order that happens.
    val actionRunner = remember(actionDispatcher, executeAction, stateManager) {
        object : HeimActionRunner {
            override fun run(actions: List<HeimAction>): Unit = run(actions, HeimStateScope.Root)

            override fun run(actions: List<HeimAction>, scope: HeimStateScope) {
                coroutineScope.launch {
                    runHeimActionSequence(
                        actions = actions,
                        dispatch = { action, onResolved ->
                            actionDispatcher.dispatch(action, stateManager, onResolved)
                        },
                        execute = { action -> executeAction(action, scope) },
                    )
                }
            }
        }
    }

    val content: @Composable () -> Unit = {
        CompositionLocalProvider(LocalHeimActionRunner provides actionRunner) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = screenState) {
                is HeimScreenState.Loading -> loadingContent?.invoke(this) ?: HeimSkeletonRenderer()

                is HeimScreenState.Content -> HeimScreenRenderer(
                    response = state.screen,
                    stateManager = stateManager,
                    onAction = handleAction,
                    customRenderer = customRenderer,
                    modifier = Modifier.fillMaxSize()
                )

                is HeimScreenState.Error -> errorContent?.invoke(this, state.message) { controller.retry() }
                    ?: HeimErrorView(
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

        // An in-app notification is another screen drawn over this one: authored, published,
        // signed and cached like any other, because to the SDK it is not a special kind of thing.
        //
        // What the host keeps is *when* it appears -- that comes from a push, a socket or a rule
        // only the app knows. What the SDK takes over is the part every app would otherwise write
        // the same way and get subtly wrong.
        overlayScreenId?.let { id ->
            HeimScreen(
                screenId = id,
                onAction = { action ->
                    if (action is DismissModalAction) onOverlayDismiss?.invoke()
                    onAction(action)
                },
                repository = repository,
                enablePullToRefresh = false,
                // Silent on both. A notification that is still loading must not cover the screen
                // underneath with a full-size skeleton, and one that failed to load is not an
                // error the user should be shown over work they were doing -- the screen they
                // are on is fine. It appears when it is ready, or it does not appear.
                errorContent = { _, _ -> },
                loadingContent = { },
                modifier = Modifier.fillMaxWidth()
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
