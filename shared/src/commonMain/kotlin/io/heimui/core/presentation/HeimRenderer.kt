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
import io.heimui.core.domain.model.component.BadgeComponent
import io.heimui.core.domain.model.component.BoxComponent
import io.heimui.core.domain.model.component.ButtonComponent
import io.heimui.core.domain.model.component.CardComponent
import io.heimui.core.domain.model.component.ContainerComponent
import io.heimui.core.domain.model.component.CustomComponent
import io.heimui.core.domain.model.component.DividerComponent
import io.heimui.core.domain.model.component.HeimComponent
import io.heimui.core.domain.model.component.IconComponent
import io.heimui.core.domain.model.component.ImageComponent
import io.heimui.core.domain.model.component.LazyColumnComponent
import io.heimui.core.domain.model.component.LazyRowComponent
import io.heimui.core.domain.model.component.SpacerComponent
import io.heimui.core.domain.model.component.SwitchComponent
import io.heimui.core.domain.model.component.TextComponent
import io.heimui.core.domain.model.component.TextFieldComponent
import io.heimui.core.domain.model.component.UnknownComponent
import io.heimui.core.presentation.component.HeimBadgeRenderer
import io.heimui.core.presentation.component.HeimBoxRenderer
import io.heimui.core.presentation.component.HeimButtonRenderer
import io.heimui.core.presentation.component.HeimCardRenderer
import io.heimui.core.presentation.component.HeimContainerRenderer
import io.heimui.core.presentation.component.HeimCustomRenderer
import io.heimui.core.presentation.component.HeimDividerRenderer
import io.heimui.core.presentation.component.HeimIconRenderer
import io.heimui.core.presentation.component.HeimImageRenderer
import io.heimui.core.presentation.component.HeimLazyColumnRenderer
import io.heimui.core.presentation.component.HeimLazyRowRenderer
import io.heimui.core.presentation.component.HeimSpacerRenderer
import io.heimui.core.presentation.component.HeimSwitchRenderer
import io.heimui.core.presentation.component.HeimTextFieldRenderer
import io.heimui.core.presentation.component.HeimTextRenderer
import io.heimui.core.presentation.component.HeimUnknownRenderer
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
            stateManager = stateManager,
            onAction = onAction,
            customRenderer = customRenderer,
            modifier = modifier
        )
        is UnknownComponent -> HeimUnknownRenderer(
            component = component,
            modifier = modifier
        )
    }
}
