package io.heimui.core.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.heimui.core.domain.evaluator.HeimValidationEngine
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.component.ButtonComponent
import io.heimui.core.domain.model.component.ButtonVariant
import io.heimui.core.domain.model.component.InputType
import io.heimui.core.domain.model.component.SwitchComponent
import io.heimui.core.domain.model.component.TextFieldComponent
import androidx.compose.material3.LocalContentColor
import io.heimui.core.presentation.accessibility.heimAccessibility
import io.heimui.core.presentation.designsystem.LocalHeimIconProvider
import io.heimui.core.presentation.action.LocalHeimActionRunner
import io.heimui.core.presentation.state.HeimStateManager
import io.heimui.core.presentation.telemetry.HeimTelemetryEvent
import io.heimui.core.presentation.telemetry.LocalHeimTelemetryObserver
import io.heimui.core.presentation.validation.LocalHeimValidatorRegistry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
internal fun HeimButtonRenderer(
    component: ButtonComponent,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var buttonModifier = modifier.heimAccessibility(component.a11y, componentId = component.id)
    if (component.isFullWidth) {
        buttonModifier = buttonModifier.fillMaxWidth()
    }

    val actionRunner = LocalHeimActionRunner.current
    val onClick = {
        if (!component.isLoading && component.isEnabled) {
            actionRunner.run(component.actions)
        }
    }

    // Style overrides, each null unless the payload asked for it. Resolved through the host's
    // brand registry, so a token follows the theme where a hex could not.
    val overriddenFill = heimColorOrNull(component.backgroundColor)
    val overriddenLabel = heimColorOrNull(component.textColor)
    val overriddenOutline = heimColorOrNull(component.borderColor)

    val variantFill = when (component.variant) {
        ButtonVariant.FILLED -> MaterialTheme.colorScheme.primary
        ButtonVariant.TONAL -> MaterialTheme.colorScheme.secondaryContainer
        ButtonVariant.OUTLINED, ButtonVariant.TEXT -> Color.Transparent
    }
    val variantLabel = when (component.variant) {
        ButtonVariant.FILLED -> MaterialTheme.colorScheme.onPrimary
        ButtonVariant.TONAL -> MaterialTheme.colorScheme.onSecondaryContainer
        ButtonVariant.OUTLINED, ButtonVariant.TEXT -> MaterialTheme.colorScheme.primary
    }

    val fill = overriddenFill ?: variantFill
    // A background on its own leaves the label to be worked out. Deriving it from luminance is
    // what keeps a custom colour from producing text nobody can read.
    val label = overriddenLabel
        ?: overriddenFill?.let { heimContentColorFor(it) }
        ?: variantLabel

    val colors = ButtonDefaults.buttonColors(
        containerColor = fill,
        contentColor = label,
        disabledContainerColor = fill.copy(alpha = HeimDisabledAlpha.CONTAINER),
        disabledContentColor = label.copy(alpha = HeimDisabledAlpha.CONTENT),
    )
    val shape = component.cornerRadius?.let { RoundedCornerShape(it.dp) } ?: ButtonDefaults.shape
    val border = overriddenOutline?.let { BorderStroke((component.borderWidth ?: 1).dp, it) }

    val iconProvider = LocalHeimIconProvider.current
    val content: @Composable RowScope.() -> Unit = {
        when {
            // The spinner takes the icon's place rather than sitting beside it, so the button
            // does not change width the moment it starts working.
            component.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            component.icon != null -> {
                iconProvider.RenderIcon(
                    name = component.icon,
                    tint = LocalContentColor.current,
                    size = 18.dp,
                    modifier = Modifier
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Text(text = component.title)
    }

    when (component.variant) {
        ButtonVariant.FILLED -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = component.isEnabled && !component.isLoading,
                shape = shape,
                colors = colors,
                border = border,
                content = content
            )
        }
        ButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = component.isEnabled && !component.isLoading,
                shape = shape,
                colors = colors,
                // Null keeps Material's own outline: an outlined button without a stated border
                // colour must still have a border.
                border = border ?: ButtonDefaults.outlinedButtonBorder(
                    enabled = component.isEnabled && !component.isLoading
                ),
                content = content
            )
        }
        ButtonVariant.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = component.isEnabled && !component.isLoading,
                shape = shape,
                colors = colors,
                border = border,
                content = content
            )
        }
        ButtonVariant.TONAL -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = component.isEnabled && !component.isLoading,
                shape = shape,
                colors = colors,
                border = border,
                content = content
            )
        }
    }
}

@Composable
internal fun HeimTextFieldRenderer(
    component: TextFieldComponent,
    stateManager: HeimStateManager,
    modifier: Modifier = Modifier
) {
    val validatorRegistry = LocalHeimValidatorRegistry.current
    val telemetry = LocalHeimTelemetryObserver.current

    // The editor owns its own TextFieldValue. Driving `value` straight from the StateFlow made
    // the text round-trip through an async boundary, which drops the TextRange and therefore the
    // caret position -- the cause of cursor jumps, dropped characters on fast typing, and broken
    // IME composition for CJK and autocorrect.
    var field by remember(component.stateKey) {
        val seed = stateManager.getValue(component.stateKey).ifEmpty { component.initialValue }
        mutableStateOf(TextFieldValue(text = seed, selection = TextRange(seed.length)))
    }
    var errorMessage by remember(component.stateKey) { mutableStateOf<String?>(null) }

    DisposableEffect(component.stateKey, component.validationRules) {
        // Registering the rules is what lets the controller gate submission on fields the user
        // never touched -- per-field validation alone only fires on typing.
        stateManager.registerField(component.stateKey, component.validationRules)
        // Only what was registered is removed; see the same guard in HeimSelectionComponents.
        onDispose {
            if (component.validationRules.isNotEmpty()) {
                stateManager.unregisterField(component.stateKey)
            }
        }
    }

    LaunchedEffect(component.stateKey, component.initialValue) {
        if (component.inputType == InputType.PASSWORD) {
            // Never let a password reach draft storage.
            stateManager.markSensitive(component.stateKey)
        }
        if (!stateManager.hasValue(component.stateKey) && component.initialValue.isNotEmpty()) {
            stateManager.updateValue(component.stateKey, component.initialValue)
        }
    }

    // Errors raised by a submit-time validation sweep, for fields never touched by the user.
    val submitErrors by stateManager.fieldErrors.collectAsState()
    val effectiveError = errorMessage ?: submitErrors[component.stateKey]

    // Adopt external mutations (draft restore, native result, another component writing the same
    // key) without clobbering what the user is currently typing.
    val externalValue by stateManager.formState
        .map { it[component.stateKey] }
        .distinctUntilChanged()
        .collectAsState(initial = field.text)

    LaunchedEffect(externalValue) {
        val incoming = externalValue
        if (incoming != null && incoming != field.text) {
            field = field.copy(text = incoming, selection = TextRange(incoming.length))
        }
    }

    val keyboardType = when (component.inputType) {
        InputType.TEXT -> KeyboardType.Text
        InputType.NUMBER -> KeyboardType.Number
        InputType.EMAIL -> KeyboardType.Email
        InputType.PASSWORD -> KeyboardType.Password
        InputType.PHONE -> KeyboardType.Phone
    }

    val visualTransformation = if (component.inputType == InputType.PASSWORD) {
        PasswordVisualTransformation()
    } else {
        VisualTransformation.None
    }

    // Style overrides, each null unless the payload asked for it. `accent_color` is the focused
    // state: an outlined field says where the caret is by colouring its own outline.
    val fieldFill = heimColorOrNull(component.backgroundColor)
    val fieldText = heimColorOrNull(component.textColor)
    val fieldOutline = heimColorOrNull(component.borderColor)
    val fieldAccent = heimColorOrNull(component.accentColor)

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = fieldText ?: MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = fieldText ?: MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = fieldFill ?: Color.Transparent,
        unfocusedContainerColor = fieldFill ?: Color.Transparent,
        focusedBorderColor = fieldAccent ?: fieldOutline ?: MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = fieldOutline ?: MaterialTheme.colorScheme.outline,
        focusedLabelColor = fieldAccent ?: MaterialTheme.colorScheme.primary,
        cursorColor = fieldAccent ?: MaterialTheme.colorScheme.primary,
    )
    val fieldShape = component.cornerRadius?.let { RoundedCornerShape(it.dp) }
        ?: OutlinedTextFieldDefaults.shape

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heimAccessibility(component.a11y, componentId = component.id)
    ) {
        OutlinedTextField(
            colors = fieldColors,
            shape = fieldShape,
            value = field,
            onValueChange = { newValue ->
                field = newValue                                   // same-frame echo to the IME
                stateManager.updateValue(component.stateKey, newValue.text)
                stateManager.clearFieldError(component.stateKey)
                errorMessage = HeimValidationEngine.validate(
                    value = newValue.text,
                    rules = component.validationRules,
                    registry = validatorRegistry,
                    onMissingValidator = { name ->
                        telemetry.onEvent(HeimTelemetryEvent.ValidatorMissing(name))
                    }
                )
            },
            modifier = Modifier.fillMaxWidth().testTag(component.id),
            label = component.label?.let { { Text(it) } },
            placeholder = component.placeholder?.let { { Text(it) } },
            isError = effectiveError != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            // Restored after being dropped in 78ef1d1: without it every field, passwords
            // included, accepts newlines.
            singleLine = component.inputType != InputType.TEXT,
            supportingText = if (effectiveError != null || component.helperText != null) {
                {
                    val message = effectiveError
                    if (message != null) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (component.helperText != null) {
                        Text(
                            text = component.helperText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else null
        )
    }
}

@Composable
internal fun HeimSwitchRenderer(
    component: SwitchComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val actionRunner = LocalHeimActionRunner.current
    val formState by stateManager.formState.collectAsState()
    val isChecked = (formState[component.stateKey] ?: component.initialChecked.toString()).toBooleanStrictOrNull()
        ?: component.initialChecked

    // Seed the declared default into form state, the way a text field seeds `initial_value`.
    // Rendering from `initialChecked` alone was not enough: the switch *looked* on while the form
    // held no value for it, so a payload interpolating `{{state.key}}` submitted null until the
    // user toggled it twice. Two components implementing the same payload concept differently is
    // the kind of thing a backend team discovers from bad data, not from a stack trace.
    LaunchedEffect(component.stateKey, component.initialChecked) {
        if (!stateManager.hasValue(component.stateKey)) {
            stateManager.updateValue(component.stateKey, component.initialChecked.toString())
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .heimAccessibility(component.a11y, componentId = component.id),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // `accent_color` is the on state, `background_color` the track while it is off. A thumb
        // is derived from the track it sits on rather than asked for, so one name is enough.
        val switchOn = heimColorOrNull(component.accentColor)
        val switchOffTrack = heimColorOrNull(component.backgroundColor)
        val switchLabel = heimColorOrNull(component.textColor)

        Text(
            text = component.label,
            style = MaterialTheme.typography.bodyLarge,
            color = switchLabel ?: Color.Unspecified,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isChecked,
            onCheckedChange = { checked ->
                stateManager.updateValue(component.stateKey, checked.toString())
                actionRunner.run(component.onCheckActions)
            },
            colors = SwitchDefaults.colors(
                checkedTrackColor = switchOn ?: MaterialTheme.colorScheme.primary,
                checkedThumbColor = switchOn?.let { heimContentColorFor(it) }
                    ?: MaterialTheme.colorScheme.onPrimary,
                uncheckedTrackColor = switchOffTrack ?: MaterialTheme.colorScheme.surfaceVariant,
                uncheckedThumbColor = switchOffTrack?.let { heimContentColorFor(it) }
                    ?: MaterialTheme.colorScheme.outline,
            )
        )
    }
}
