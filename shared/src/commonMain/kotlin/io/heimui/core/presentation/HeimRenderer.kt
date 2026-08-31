package io.heimui.core.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.heimui.core.domain.evaluator.HeimConditionEvaluator
import io.heimui.core.domain.model.HeimScreenResponse
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.component.*
import io.heimui.core.presentation.component.*
import io.heimui.core.presentation.state.HeimStateManager

@Composable
fun HeimScreenRenderer(
    response: HeimScreenResponse,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier,
    customRenderer: (@Composable (CustomComponent) -> Unit)? = null
) {
    val screenModifier = if (response.applySafeInsets) {
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    } else {
        modifier.fillMaxSize()
    }

    Box(modifier = screenModifier) {
        HeimRenderer(
            component = response.root,
            stateManager = stateManager,
            onAction = onAction,
            customRenderer = customRenderer
        )
    }
}

@Composable
fun HeimRenderer(
    component: HeimComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier,
    customRenderer: (@Composable (CustomComponent) -> Unit)? = null
) {
    val formState by stateManager.formState.collectAsState()
    val isVisible = HeimConditionEvaluator.evaluate(component.visibleIf, formState)

    if (!isVisible) return

    when (component) {
        is ContainerComponent -> HeimContainerRenderer(
            component = component,
            stateManager = stateManager,
            onAction = onAction,
            modifier = modifier
        )
        is BoxComponent -> HeimBoxRenderer(
            component = component,
            stateManager = stateManager,
            onAction = onAction,
            modifier = modifier
        )
        is LazyColumnComponent -> HeimLazyColumnRenderer(
            component = component,
            stateManager = stateManager,
            onAction = onAction,
            modifier = modifier
        )
        is LazyRowComponent -> HeimLazyRowRenderer(
            component = component,
            stateManager = stateManager,
            onAction = onAction,
            modifier = modifier
        )
        is TextComponent -> HeimTextRenderer(
            component = component,
            modifier = modifier
        )
        is ImageComponent -> HeimImageRenderer(
            component = component,
            modifier = modifier
        )
        is CardComponent -> HeimCardRenderer(
            component = component,
            stateManager = stateManager,
            onAction = onAction,
            modifier = modifier
        )
        is BadgeComponent -> HeimBadgeRenderer(
            component = component,
            modifier = modifier
        )
        is ButtonComponent -> HeimButtonRenderer(
            component = component,
            onAction = onAction,
            modifier = modifier
        )
        is TextFieldComponent -> HeimTextFieldRenderer(
            component = component,
            stateManager = stateManager,
            modifier = modifier
        )
        is SwitchComponent -> HeimSwitchRenderer(
            component = component,
            stateManager = stateManager,
            onAction = onAction,
            modifier = modifier
        )
        is IconComponent -> HeimIconRenderer(
            component = component,
            modifier = modifier
        )
        is SpacerComponent -> HeimSpacerRenderer(
            component = component,
            modifier = modifier
        )
        is DividerComponent -> HeimDividerRenderer(
            component = component,
            modifier = modifier
        )
        is CustomComponent -> HeimCustomRenderer(
            component = component,
            customRenderer = customRenderer,
            modifier = modifier
        )
        is UnknownComponent -> HeimUnknownRenderer(
            component = component,
            modifier = modifier
        )
    }
}
