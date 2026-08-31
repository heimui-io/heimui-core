package io.heimui.core.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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

@Composable
fun HeimButtonRenderer(
    component: ButtonComponent,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var buttonModifier = modifier.heimAccessibility(component.a11y)
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

@Composable
fun HeimTextFieldRenderer(
    component: TextFieldComponent,
    stateManager: HeimStateManager,
    modifier: Modifier = Modifier
) {
    val formState by stateManager.formState.collectAsState()
    val currentValue = formState[component.stateKey] ?: component.initialValue

    // Initial registration in state if empty
    LaunchedEffect(component.stateKey) {
        if (!formState.containsKey(component.stateKey) && component.initialValue.isNotEmpty()) {
            stateManager.updateValue(component.stateKey, component.initialValue)
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            .heimAccessibility(component.a11y)
    ) {
        OutlinedTextField(
            value = currentValue,
            onValueChange = { newValue ->
                stateManager.updateValue(component.stateKey, newValue)
                errorMessage = HeimValidationEngine.validate(newValue, component.validationRules)
            },
            modifier = Modifier.fillMaxWidth(),
            label = component.label?.let { { Text(it) } },
            placeholder = component.placeholder?.let { { Text(it) } },
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            singleLine = component.inputType != InputType.TEXT,
            supportingText = {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
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
        )
    }
}

@Composable
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
            .heimAccessibility(component.a11y),
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
