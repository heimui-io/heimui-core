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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import io.heimui.core.presentation.accessibility.heimAccessibility
import io.heimui.core.presentation.state.HeimStateManager
import io.heimui.core.presentation.telemetry.HeimTelemetryEvent
import io.heimui.core.presentation.telemetry.LocalHeimTelemetryObserver
import io.heimui.core.presentation.validation.LocalHeimValidatorRegistry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal @Composable
fun HeimButtonRenderer(
    component: ButtonComponent,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var buttonModifier = modifier.heimAccessibility(component.a11y, componentId = component.id)
    if (component.isFullWidth) {
        buttonModifier = buttonModifier.fillMaxWidth()
    }

    val onClick = {
        if (!component.isLoading && component.isEnabled) {
            component.actions.forEach { action ->
                onAction(action)
            }
        }
    }

    val content: @Composable RowScope.() -> Unit = {
        if (component.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = component.title)
    }

    when (component.variant) {
        ButtonVariant.FILLED -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = component.isEnabled && !component.isLoading,
                content = content
            )
        }
        ButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = component.isEnabled && !component.isLoading,
                content = content
            )
        }
        ButtonVariant.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = component.isEnabled && !component.isLoading,
                content = content
            )
        }
        ButtonVariant.TONAL -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = component.isEnabled && !component.isLoading,
                content = content
            )
        }
    }
}

internal @Composable
fun HeimTextFieldRenderer(
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
        onDispose { stateManager.unregisterField(component.stateKey) }
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heimAccessibility(component.a11y, componentId = component.id)
    ) {
        OutlinedTextField(
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

internal @Composable
fun HeimSwitchRenderer(
    component: SwitchComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by stateManager.formState.collectAsState()
    val isChecked = (formState[component.stateKey] ?: component.initialChecked.toString()).toBooleanStrictOrNull()
        ?: component.initialChecked

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .heimAccessibility(component.a11y, componentId = component.id),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = component.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isChecked,
            onCheckedChange = { checked ->
                stateManager.updateValue(component.stateKey, checked.toString())
                component.onCheckActions.forEach { action ->
                    onAction(action)
                }
            }
        )
    }
}
