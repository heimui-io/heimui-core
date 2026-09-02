package io.heimui.core.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import io.heimui.core.presentation.component.heimSize
import io.heimui.core.domain.model.component.CheckboxComponent
import io.heimui.core.domain.model.component.ChipComponent
import io.heimui.core.domain.model.component.RichTextComponent
import io.heimui.core.domain.model.component.DatePickerComponent
import io.heimui.core.domain.model.component.RadioGroupComponent
import io.heimui.core.domain.model.component.SelectComponent
import io.heimui.core.presentation.component.HeimBoxRenderer
import io.heimui.core.presentation.component.HeimCheckboxRenderer
import io.heimui.core.presentation.component.HeimChipRenderer
import io.heimui.core.presentation.component.HeimRichTextRenderer
import io.heimui.core.presentation.component.HeimDatePickerRenderer
import io.heimui.core.presentation.component.HeimRadioGroupRenderer
import io.heimui.core.presentation.component.HeimSelectRenderer
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
public fun HeimScreenRenderer(
    response: HeimScreenResponse,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier,
    customRenderer: (@Composable (CustomComponent) -> Unit)? = null
) {
    val contentModifier = if (response.applySafeInsets) {
        modifier.windowInsetsPadding(WindowInsets.safeDrawing)
    } else {
        modifier
    }

    Box(modifier = contentModifier.fillMaxSize()) {
        HeimRenderer(
            component = response.root,
            stateManager = stateManager,
            onAction = onAction,
            customRenderer = customRenderer
        )
    }
}

/**
 * Visibility for one node, subscribed to only the state keys its own expression references.
 *
 * Subscribing every node to the whole form map (the previous behaviour) meant a single keystroke
 * recomposed the entire tree. Slicing keeps the blast radius to the nodes that actually depend on
 * the key that changed.
 */
@Composable
internal fun rememberVisibility(visibleIf: String?, stateManager: HeimStateManager): Boolean {
    if (visibleIf == null) return true
    val keys = remember(visibleIf) { HeimConditionEvaluator.referencedKeys(visibleIf) }

    // Seeded from the current state rather than emptyMap(): with a fail-closed evaluator an empty
    // initial slice makes every conditional node invisible for one frame, which reads as a flicker
    // on every screen load.
    val initialSlice = remember(visibleIf, stateManager) {
        stateManager.formState.value.let { state -> keys.associateWith { state[it].orEmpty() } }
    }
    val slice by remember(visibleIf, stateManager) {
        stateManager.formState
            .map { state -> keys.associateWith { state[it].orEmpty() } }
            .distinctUntilChanged()
    }.collectAsState(initial = initialSlice)

    return remember(visibleIf, slice) { HeimConditionEvaluator.evaluate(visibleIf, slice) }
}

@Composable
public fun HeimRenderer(
    component: HeimComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier,
    customRenderer: (@Composable (CustomComponent) -> Unit)? = null
) {
    val isVisible = rememberVisibility(component.visibleIf, stateManager)

    if (!isVisible) return

    // One place for every component's dimensions, so adding a constraint later does not mean
    // touching sixteen renderers.
    val modifier = modifier.heimSize(component.frame)

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
        is RichTextComponent -> HeimRichTextRenderer(
            component = component,
            modifier = modifier
        )

        is ChipComponent -> HeimChipRenderer(
            component = component,
            stateManager = stateManager,
            modifier = modifier
        )

        is CheckboxComponent -> HeimCheckboxRenderer(
            component = component,
            stateManager = stateManager,
            modifier = modifier
        )

        is RadioGroupComponent -> HeimRadioGroupRenderer(
            component = component,
            stateManager = stateManager,
            modifier = modifier
        )

        is SelectComponent -> HeimSelectRenderer(
            component = component,
            stateManager = stateManager,
            modifier = modifier
        )

        is DatePickerComponent -> HeimDatePickerRenderer(
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
