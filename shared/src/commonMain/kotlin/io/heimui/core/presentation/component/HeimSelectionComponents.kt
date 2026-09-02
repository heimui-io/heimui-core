package io.heimui.core.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.heimui.core.domain.model.component.CheckboxComponent
import io.heimui.core.domain.model.component.DatePickerComponent
import io.heimui.core.domain.model.component.RadioGroupComponent
import io.heimui.core.domain.model.component.SelectComponent
import io.heimui.core.presentation.accessibility.heimAccessibility
import io.heimui.core.presentation.action.LocalHeimActionRunner
import io.heimui.core.presentation.state.HeimStateManager
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Registers a field's rules and seeds its declared default into form state.
 *
 * Every input needs both, and getting one of them wrong is invisible: a field that never
 * registers is skipped by the submit-time validation sweep, and one that never seeds submits null
 * for a default the payload plainly declared. Both were real bugs before this was shared.
 */
@Composable
private fun rememberFieldRegistration(
    stateManager: HeimStateManager,
    stateKey: String,
    rules: List<io.heimui.core.domain.model.validation.ValidationRule>,
    initialValue: String,
) {
    DisposableEffect(stateKey, rules) {
        stateManager.registerField(stateKey, rules)
        onDispose { stateManager.unregisterField(stateKey) }
    }
    LaunchedEffect(stateKey, initialValue) {
        if (!stateManager.hasValue(stateKey) && initialValue.isNotEmpty()) {
            stateManager.updateValue(stateKey, initialValue)
        }
    }
}

@Composable
internal fun HeimCheckboxRenderer(
    component: CheckboxComponent,
    stateManager: HeimStateManager,
    modifier: Modifier = Modifier,
) {
    val actionRunner = LocalHeimActionRunner.current
    val formState by stateManager.formState.collectAsState()
    val isChecked = formState[component.stateKey]?.toBooleanStrictOrNull() ?: component.initialChecked

    rememberFieldRegistration(
        stateManager = stateManager,
        stateKey = component.stateKey,
        rules = component.validationRules,
        initialValue = component.initialChecked.toString(),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            // The whole row toggles, not just the box. A 20dp target beside a long label is the
            // classic accessibility failure of a checkbox, and `toggleable` also gives the row
            // the right semantics for a screen reader instead of announcing two separate things.
            .toggleable(
                value = isChecked,
                role = Role.Checkbox,
                onValueChange = { checked ->
                    stateManager.updateValue(component.stateKey, checked.toString())
                    actionRunner.run(component.onCheckActions)
                },
            )
            .padding(vertical = 8.dp)
            .heimAccessibility(component.a11y, componentId = component.id),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = isChecked, onCheckedChange = null)
        Text(
            text = component.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
internal fun HeimRadioGroupRenderer(
    component: RadioGroupComponent,
    stateManager: HeimStateManager,
    modifier: Modifier = Modifier,
) {
    val actionRunner = LocalHeimActionRunner.current
    val formState by stateManager.formState.collectAsState()
    val selected = formState[component.stateKey] ?: component.initialValue

    rememberFieldRegistration(
        stateManager = stateManager,
        stateKey = component.stateKey,
        rules = component.validationRules,
        initialValue = component.initialValue,
    )

    Column(
        // `selectableGroup` is what makes a screen reader announce "2 of 4" instead of reading
        // four unrelated radio buttons.
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
            .heimAccessibility(component.a11y, componentId = component.id),
    ) {
        component.label?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        component.options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = option.value == selected,
                        role = Role.RadioButton,
                        onClick = {
                            stateManager.updateValue(component.stateKey, option.value)
                            actionRunner.run(component.onSelectActions)
                        },
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option.value == selected, onClick = null)
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HeimSelectRenderer(
    component: SelectComponent,
    stateManager: HeimStateManager,
    modifier: Modifier = Modifier,
) {
    val actionRunner = LocalHeimActionRunner.current
    val formState by stateManager.formState.collectAsState()
    val selectedValue = formState[component.stateKey] ?: component.initialValue
    var expanded by remember { mutableStateOf(false) }

    rememberFieldRegistration(
        stateManager = stateManager,
        stateKey = component.stateKey,
        rules = component.validationRules,
        initialValue = component.initialValue,
    )

    // The label of the stored value, not the value itself. A payload storing country codes should
    // still show "Colombia" — and a stored value with no matching option shows as empty rather
    // than leaking a raw code the user never chose.
    val selectedLabel = component.options.firstOrNull { it.value == selectedValue }?.label.orEmpty()
    val submitErrors by stateManager.fieldErrors.collectAsState()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.heimAccessibility(component.a11y, componentId = component.id),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = component.label?.let { { Text(it) } },
            placeholder = component.placeholder?.let { { Text(it) } },
            isError = submitErrors[component.stateKey] != null,
            supportingText = submitErrors[component.stateKey]?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            component.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        stateManager.updateValue(component.stateKey, option.value)
                        expanded = false
                        actionRunner.run(component.onSelectActions)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HeimDatePickerRenderer(
    component: DatePickerComponent,
    stateManager: HeimStateManager,
    modifier: Modifier = Modifier,
) {
    val actionRunner = LocalHeimActionRunner.current
    val formState by stateManager.formState.collectAsState()
    val storedIso = formState[component.stateKey] ?: component.initialValue
    var showDialog by remember { mutableStateOf(false) }

    rememberFieldRegistration(
        stateManager = stateManager,
        stateKey = component.stateKey,
        rules = component.validationRules,
        initialValue = component.initialValue,
    )

    val submitErrors by stateManager.fieldErrors.collectAsState()

    // A read-only OutlinedTextField still consumes its own touches, so a `clickable` wrapped
    // around it never fires — the field takes focus and nothing opens. Watching the interaction
    // source is the way to hear the tap the field swallowed.
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) showDialog = true
        }
    }

    OutlinedTextField(
        value = storedIso,
        onValueChange = {},
        readOnly = true,
        label = component.label?.let { { Text(it) } },
        placeholder = component.placeholder?.let { { Text(it) } },
        isError = submitErrors[component.stateKey] != null,
        supportingText = submitErrors[component.stateKey]?.let { { Text(it) } },
        // No trailing icon: the SDK ships no icon dependency, which is the reason
        // HeimIconProvider exists. A calendar glyph here would be the app's to supply.
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .heimAccessibility(component.a11y, componentId = component.id),
    )

    if (showDialog) {
        val min = component.minDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val max = component.maxDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val stored = storedIso.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it).toUtcMillis() }.getOrNull() }

        val state = rememberDatePickerState(
            initialSelectedDateMillis = stored,
            // Open where something is actually selectable. A birth-date field bounded to 2007
            // opened on the current month with every day greyed out, which reads as a broken
            // picker rather than as a constraint — the user has no idea to scroll back 18 years.
            initialDisplayedMonthMillis = stored
                ?: max?.toUtcMillis()
                ?: min?.toUtcMillis(),
            selectableDates = remember(min, max) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val date = utcTimeMillis.toUtcLocalDate()
                        return (min == null || date >= min) && (max == null || date <= max)
                    }
                }
            },
        )

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            // Stored as ISO, never as the localised text the user saw: the value
                            // travels to a backend, and "15/03/2024" is ambiguous across locales.
                            stateManager.updateValue(
                                component.stateKey,
                                millis.toUtcLocalDate().toString(),
                            )
                        }
                        showDialog = false
                        actionRunner.run(component.onSelectActions)
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

/**
 * Material's picker speaks UTC milliseconds; the payload speaks ISO dates.
 *
 * UTC on both sides deliberately. Converting through the device's zone would move a birth date by
 * a day for anyone west of Greenwich — a calendar date has no time zone, and treating it as an
 * instant is the classic off-by-one in date pickers.
 */
private fun LocalDate.toUtcMillis(): Long =
    atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toUtcLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date
