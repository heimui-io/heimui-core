package io.heimui.core.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import io.heimui.core.domain.evaluator.HeimValidatorRegistry
import io.heimui.core.domain.model.HeimScreenResponse
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.action.ShowBottomSheetAction
import io.heimui.core.domain.model.action.ShowDialogAction
import io.heimui.core.domain.model.action.SubmitFormAction
import io.heimui.core.domain.model.component.ContainerComponent
import io.heimui.core.domain.model.component.HeimComponent
import io.heimui.core.domain.repository.HeimScreenRepository
import io.heimui.core.domain.repository.HeimScreenResult
import io.heimui.core.domain.repository.HeimSubmitResult
import io.heimui.core.presentation.telemetry.HeimTelemetryEvent
import io.heimui.core.presentation.telemetry.HeimTelemetryObserver
import io.heimui.core.presentation.telemetry.NoOpHeimTelemetryObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/**
 * State holder for one screen: loading pipeline, refresh, modals and form submission.
 *
 * Extracted from `HeimScreen` for two reasons. First, all of that state used to live in plain
 * `remember`, so a configuration change dropped back to Loading, refetched, and closed any open
 * dialog. Second, none of it was reachable from a unit test -- the whole pipeline could only be
 * exercised through Compose UI testing, which this project has no harness for.
 *
 * Deliberately a plain class rather than an `androidx.lifecycle.ViewModel`: this is an SDK, and
 * requiring a `ViewModelStoreOwner` would impose a lifecycle dependency on every host, which is
 * particularly awkward on iOS. Hosts that want ViewModel semantics can hold one of these inside
 * their own.
 */
public class HeimScreenController(
    private val screenId: String,
    private val repository: HeimScreenRepository,
    private val scope: CoroutineScope,
    private var queryParams: Map<String, String> = emptyMap(),
    private val telemetryObserver: HeimTelemetryObserver = NoOpHeimTelemetryObserver,
    private val timeSource: TimeSource = TimeSource.Monotonic
) {
    private val _screenState: MutableState<HeimScreenState> = mutableStateOf(HeimScreenState.Loading)
    public val screenState: State<HeimScreenState> = _screenState

    private val _isRefreshing = mutableStateOf(false)
    public val isRefreshing: State<Boolean> = _isRefreshing

    private val _isSubmitting = mutableStateOf(false)
    public val isSubmitting: State<Boolean> = _isSubmitting

    private val _activeBottomSheet = mutableStateOf<ShowBottomSheetAction?>(null)
    public val activeBottomSheet: State<ShowBottomSheetAction?> = _activeBottomSheet

    private val _activeDialog = mutableStateOf<ShowDialogAction?>(null)
    public val activeDialog: State<ShowDialogAction?> = _activeDialog

    private var loadJob: Job? = null

    public fun updateQueryParams(params: Map<String, String>) {
        queryParams = params
    }

    /** Starts (or restarts) the load. [resetToLoading] is false for a pull-to-refresh. */
    public fun load(resetToLoading: Boolean = true) {
        loadJob?.cancel()
        if (resetToLoading && _screenState.value !is HeimScreenState.Content) {
            _screenState.value = HeimScreenState.Loading
        }
        loadJob = scope.launch {
            try {
                repository.getScreen(screenId, queryParams).collectLatest { result ->
                    _isRefreshing.value = false
                    apply(result)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    public fun retry() {
        _screenState.value = HeimScreenState.Loading
        load(resetToLoading = true)
    }

    public fun refresh() {
        _isRefreshing.value = true
        load(resetToLoading = false)
    }

    private fun apply(result: HeimScreenResult) {
        when (result) {
            is HeimScreenResult.Success -> {
                _screenState.value = contentOrEmpty(result.screen, result.isStale)
                telemetryObserver.onEvent(
                    HeimTelemetryEvent.ScreenViewed(screenId, result.isStale)
                )
                if (result.screen.violations.isNotEmpty()) {
                    telemetryObserver.onEvent(
                        HeimTelemetryEvent.PayloadViolation(screenId, result.screen.violations)
                    )
                }
            }

            is HeimScreenResult.Stale -> {
                // Content stays on screen: replacing readable cached content with an error page
                // leaves an offline user worse off than before they pulled to refresh.
                _screenState.value = contentOrEmpty(result.screen, isStale = true)
                telemetryObserver.onEvent(
                    HeimTelemetryEvent.ScreenRefreshFailed(screenId, result.reason)
                )
            }

            is HeimScreenResult.Error -> {
                _screenState.value = HeimScreenState.Error(result.message, result.throwable)
                telemetryObserver.onEvent(
                    HeimTelemetryEvent.ScreenError(screenId, result.message, result.throwable)
                )
            }
        }
    }

    private fun contentOrEmpty(screen: HeimScreenResponse, isStale: Boolean): HeimScreenState =
        if (screen.root.isRenderableContent()) {
            HeimScreenState.Content(screen, isStale)
        } else {
            HeimScreenState.Empty()
        }

    private fun HeimComponent.isRenderableContent(): Boolean =
        this !is ContainerComponent || children.isNotEmpty()

    public fun showBottomSheet(action: ShowBottomSheetAction) {
        _activeBottomSheet.value = action
    }

    public fun showDialog(action: ShowDialogAction) {
        _activeDialog.value = action
    }

    public fun dismissBottomSheet() {
        _activeBottomSheet.value = null
    }

    public fun dismissDialog() {
        _activeDialog.value = null
    }

    public fun dismissModals() {
        _activeBottomSheet.value = null
        _activeDialog.value = null
    }

    /**
     * Executes a [SubmitFormAction] end to end.
     *
     * The SDK previously never called `submitForm` at all: the action fell through to the host,
     * so neither the validation gate nor `{{state.*}}` interpolation ever ran. Both happen here.
     *
     * @return true when the submission was attempted (i.e. validation passed).
     */
    public suspend fun submitForm(
        action: SubmitFormAction,
        stateManager: HeimStateManager,
        validatorRegistry: HeimValidatorRegistry = HeimValidatorRegistry.default,
        onHostAction: (HeimAction) -> Unit = {}
    ): Boolean {
        // Per-field validation only runs on typing, so a required field the user never touched
        // has never evaluated its rules. Gate on the whole form before touching the network.
        val errors = stateManager.validateForm(
            registry = validatorRegistry,
            onMissingValidator = { name ->
                telemetryObserver.onEvent(HeimTelemetryEvent.ValidatorMissing(name))
            }
        )
        if (errors.isNotEmpty()) {
            telemetryObserver.onEvent(
                HeimTelemetryEvent.FormSubmitted(action.endpoint, success = false, durationMs = 0)
            )
            return false
        }

        val unresolved = mutableListOf<String>()
        val payload = stateManager.interpolatePayload(action.payload) { unresolved += it }
        if (unresolved.isNotEmpty()) {
            telemetryObserver.onEvent(
                HeimTelemetryEvent.PayloadViolation(
                    screenId,
                    unresolved.map { "Unresolved state placeholder '$it' in submit payload" }
                )
            )
        }

        _isSubmitting.value = true
        val started = timeSource.markNow()
        try {
            val result = repository.submitForm(action.endpoint, action.method, payload)
            val elapsed = started.elapsedNow().inWholeMilliseconds
            when (result) {
                is HeimSubmitResult.Success -> {
                    telemetryObserver.onEvent(
                        HeimTelemetryEvent.FormSubmitted(action.endpoint, true, elapsed)
                    )
                    // A server that answers a submission with a screen replaces the current one.
                    result.responseScreen?.let {
                        _screenState.value = contentOrEmpty(it, isStale = false)
                    }
                }

                is HeimSubmitResult.Error -> {
                    telemetryObserver.onEvent(
                        HeimTelemetryEvent.FormSubmitted(action.endpoint, false, elapsed)
                    )
                    telemetryObserver.onEvent(
                        HeimTelemetryEvent.ScreenError(screenId, result.message, result.throwable)
                    )
                }

                is HeimSubmitResult.Blocked -> {
                    // Refused locally by the security policy; it never left the device.
                    telemetryObserver.onEvent(
                        HeimTelemetryEvent.SubmissionBlocked(action.endpoint, result.message)
                    )
                }
            }
            onHostAction(action)
            return true
        } finally {
            _isSubmitting.value = false
        }
    }
}

/**
 * Remembers a [HeimScreenController] that survives configuration changes.
 *
 * The controller instance itself is `remember`ed; the pieces of state that can be serialised
 * (whether content was already loaded, which modal was open) ride along in `rememberSaveable`
 * so a rotation does not restart the screen.
 */
@Composable
public fun rememberHeimScreenController(
    screenId: String,
    repository: HeimScreenRepository,
    queryParams: Map<String, String> = emptyMap(),
    telemetryObserver: HeimTelemetryObserver = NoOpHeimTelemetryObserver
): HeimScreenController {
    val scope = rememberCoroutineScope()
    val controller = remember(screenId, repository) {
        HeimScreenController(
            screenId = screenId,
            repository = repository,
            scope = scope,
            queryParams = queryParams,
            telemetryObserver = telemetryObserver
        )
    }
    controller.updateQueryParams(queryParams)

    // Survives Activity recreation: on the way back the cache answers immediately, so the user
    // sees content instead of the skeleton flashing.
    var hasLoadedBefore by rememberSaveable(screenId) { mutableStateOf(false) }
    LaunchedEffect(controller) {
        if (hasLoadedBefore) {
            controller.load(resetToLoading = false)
        }
        hasLoadedBefore = true
    }

    return controller
}
