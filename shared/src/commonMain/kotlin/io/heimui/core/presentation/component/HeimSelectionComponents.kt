package io.heimui.core.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.heimui.core.domain.model.component.CheckboxComponent
import io.heimui.core.domain.model.component.ChipComponent
import io.heimui.core.domain.model.component.ChipVariant
import io.heimui.core.domain.model.component.DatePickerComponent
import io.heimui.core.domain.model.component.RadioGroupComponent
import io.heimui.core.domain.model.component.RadioComponent
import io.heimui.core.domain.model.component.SelectComponent
import io.heimui.core.presentation.accessibility.heimAccessibility
import io.heimui.core.presentation.action.LocalHeimActionRunner
import io.heimui.core.presentation.designsystem.LocalHeimIconProvider
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
        // Guarded the same way `registerField` is: it returns early for a field with no rules,
        // so an unguarded unregister removes what somebody else put there. `radio` is the first
        // component where that can happen -- several radios share one `state_key`, each registers
        // nothing, and any one of them leaving the composition (scrolled out of a lazy list,
        // hidden by a `visible_if`) would drop the rules a sibling registered for that key.
        onDispose { if (rules.isNotEmpty()) stateManager.unregisterField(stateKey) }
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
    val stateKey = rememberScopedStateKey(component.stateKey)
    val actionRunner = LocalHeimActionRunner.current
    val formState by stateManager.formState.collectAsState()
    val isChecked = formState[stateKey]?.toBooleanStrictOrNull() ?: component.initialChecked

    rememberFieldRegistration(
        stateManager = stateManager,
        stateKey = stateKey,
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
                    stateManager.updateValue(stateKey, checked.toString())
                    actionRunner.run(component.onCheckActions)
                },
            )
            .padding(vertical = 8.dp)
            .heimAccessibility(component.a11y, componentId = component.id),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // `accent_color` fills the box when it is checked; the tick is derived from it so it can
        // always be seen against whatever the author chose.
        val boxChecked = heimColorOrNull(component.accentColor)
        val boxOutline = heimColorOrNull(component.borderColor)
        val boxLabel = heimColorOrNull(component.textColor)

        Checkbox(
            checked = isChecked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = boxChecked ?: MaterialTheme.colorScheme.primary,
                uncheckedColor = boxOutline ?: MaterialTheme.colorScheme.onSurfaceVariant,
                checkmarkColor = boxChecked?.let { heimContentColorFor(it) }
                    ?: MaterialTheme.colorScheme.onPrimary,
            ),
        )
        Text(
            text = component.label,
            style = MaterialTheme.typography.bodyLarge,
            color = boxLabel ?: Color.Unspecified,
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
    val stateKey = rememberScopedStateKey(component.stateKey)
    val actionRunner = LocalHeimActionRunner.current
    val formState by stateManager.formState.collectAsState()
    val selected = formState[stateKey] ?: component.initialValue

    rememberFieldRegistration(
        stateManager = stateManager,
        stateKey = stateKey,
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
                            stateManager.updateValue(stateKey, option.value)
                            actionRunner.run(component.onSelectActions)
                        },
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = option.value == selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = heimColorOrNull(component.accentColor)
                            ?: MaterialTheme.colorScheme.primary,
                        unselectedColor = heimColorOrNull(component.borderColor)
                            ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = heimColorOrNull(component.textColor) ?: Color.Unspecified,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
internal fun HeimRadioRenderer(
    component: RadioComponent,
    stateManager: HeimStateManager,
    modifier: Modifier = Modifier,
) {
    val stateKey = rememberScopedStateKey(component.stateKey)
    val actionRunner = LocalHeimActionRunner.current
    val formState by stateManager.formState.collectAsState()

    val initialVal = if (component.initialSelected) component.value else ""
    rememberFieldRegistration(
        stateManager = stateManager,
        stateKey = stateKey,
        rules = emptyList(),
        initialValue = initialVal,
    )

    val currentVal = formState[stateKey] ?: initialVal
    val isSelected = currentVal == component.value

    val radioColors = RadioButtonDefaults.colors(
        selectedColor = heimColorOrNull(component.accentColor)
            ?: MaterialTheme.colorScheme.primary,
        unselectedColor = heimColorOrNull(component.borderColor)
            ?: MaterialTheme.colorScheme.onSurfaceVariant,
    )

    val onSelect = {
        stateManager.updateValue(stateKey, component.value)
        actionRunner.run(component.onSelectActions)
    }

    if (component.label.isNullOrBlank()) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = radioColors,
            modifier = modifier.heimAccessibility(component.a11y, componentId = component.id),
        )
    } else {
        Row(
            modifier = modifier
                .selectable(
                    selected = isSelected,
                    role = Role.RadioButton,
                    onClick = onSelect,
                )
                .heimAccessibility(component.a11y, componentId = component.id),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = radioColors,
            )
            Text(
                text = component.label,
                style = MaterialTheme.typography.bodyLarge,
                color = heimColorOrNull(component.textColor) ?: Color.Unspecified,
                modifier = Modifier.padding(start = 8.dp),
            )
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
    val stateKey = rememberScopedStateKey(component.stateKey)
    val actionRunner = LocalHeimActionRunner.current
    val formState by stateManager.formState.collectAsState()
    val selectedValue = formState[stateKey] ?: component.initialValue
    var expanded by remember { mutableStateOf(false) }

    rememberFieldRegistration(
        stateManager = stateManager,
        stateKey = stateKey,
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
            isError = submitErrors[stateKey] != null,
            supportingText = submitErrors[stateKey]?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = component.cornerRadius?.let { RoundedCornerShape(it.dp) }
                ?: OutlinedTextFieldDefaults.shape,
            colors = heimOutlinedFieldColors(
                fill = heimColorOrNull(component.backgroundColor),
                text = heimColorOrNull(component.textColor),
                outline = heimColorOrNull(component.borderColor),
                accent = heimColorOrNull(component.accentColor),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            component.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        stateManager.updateValue(stateKey, option.value)
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
    val stateKey = rememberScopedStateKey(component.stateKey)
    val actionRunner = LocalHeimActionRunner.current
    val formState by stateManager.formState.collectAsState()
    val storedIso = formState[stateKey] ?: component.initialValue
    var showDialog by remember { mutableStateOf(false) }

    rememberFieldRegistration(
        stateManager = stateManager,
        stateKey = stateKey,
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

    // The accent follows the value into the dialog: a field outlined in the brand colour that
    // opens a calendar highlighting the day in Material purple looks like two different products.
    val accent = heimColorOrNull(component.accentColor)

    OutlinedTextField(
        value = storedIso,
        onValueChange = {},
        readOnly = true,
        label = component.label?.let { { Text(it) } },
        placeholder = component.placeholder?.let { { Text(it) } },
        isError = submitErrors[stateKey] != null,
        supportingText = submitErrors[stateKey]?.let { { Text(it) } },
        // No trailing icon: the SDK ships no icon dependency, which is the reason
        // HeimIconProvider exists. A calendar glyph here would be the app's to supply.
        interactionSource = interactionSource,
        shape = component.cornerRadius?.let { RoundedCornerShape(it.dp) }
            ?: OutlinedTextFieldDefaults.shape,
        colors = heimOutlinedFieldColors(
            fill = heimColorOrNull(component.backgroundColor),
            text = heimColorOrNull(component.textColor),
            outline = heimColorOrNull(component.borderColor),
            accent = accent,
        ),
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
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = accent ?: MaterialTheme.colorScheme.primary,
                selectedDayContentColor = accent?.let { heimContentColorFor(it) }
                    ?: MaterialTheme.colorScheme.onPrimary,
                todayDateBorderColor = accent ?: MaterialTheme.colorScheme.primary,
            ),
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            // Stored as ISO, never as the localised text the user saw: the value
                            // travels to a backend, and "15/03/2024" is ambiguous across locales.
                            stateManager.updateValue(
                                stateKey,
                                millis.toUtcLocalDate().toString(),
                            )
                        }
                        showDialog = false
                        actionRunner.run(component.onSelectActions)
                    },
                ) { Text(component.confirmText) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(component.dismissText) }
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

@Composable
internal fun HeimChipRenderer(
    component: ChipComponent,
    stateManager: HeimStateManager,
    modifier: Modifier = Modifier,
) {
    val stateKey = component.stateKey?.let { rememberScopedStateKey(it) }
    val actionRunner = LocalHeimActionRunner.current
    val iconProvider = LocalHeimIconProvider.current
    val formState by stateManager.formState.collectAsState()

    val stored = stateKey?.let { formState[it] }
    val isSelected = when {
        stateKey == null -> false
        // With a value, the chips sharing a key are one choice: this one is on when the stored
        // value is its own.
        component.value != null -> stored == component.value
        // Without one, the chip is its own on/off.
        else -> stored?.toBooleanStrictOrNull() ?: false
    }

    val onClick = {
        stateKey?.let { key ->
            val next = when {
                // Tapping the selected chip clears the group. A single-choice row with no way to
                // undo traps the user on their first tap.
                component.value != null -> if (isSelected) "" else component.value
                else -> (!isSelected).toString()
            }
            stateManager.updateValue(key, next)
        }
        actionRunner.run(component.actions)
    }

    /**
     * One line, and an ellipsis when the width runs out.
     *
     * Compose's default is as many lines as the text needs, which is right for a paragraph and
     * wrong for a chip: a long label made the chip taller than the ones beside it and pushed the
     * height of the whole row. Material calls a chip a compact single-line element, so this is
     * not a limit being imposed -- it is a default meant for running text being removed.
     *
     * `maxLines` alone would cut mid-glyph with nothing to show for it; the overflow is what
     * draws the ellipsis. Neither truncates anything on its own: a chip is only ever cut when
     * something gives it a maximum width -- a `frame`, or a parent that has run out of room. In a
     * `lazy_row` it simply grows wide, which is what a scrolling row of filters is for.
     */
    val label: @Composable () -> Unit = {
        Text(component.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    val leadingIcon: (@Composable () -> Unit)? = component.icon?.let { name ->
        {
            iconProvider.RenderIcon(
                name = name,
                tint = LocalContentColor.current,
                size = FilterChipDefaults.IconSize,
                modifier = Modifier,
            )
        }
    }
    val chipModifier = modifier.heimAccessibility(component.a11y, componentId = component.id)

    // `accent_color` is the selected fill of a filter chip; `background_color` is the resting one.
    val chipFill = heimColorOrNull(component.backgroundColor)
    val chipSelected = heimColorOrNull(component.accentColor)
    val chipLabel = heimColorOrNull(component.textColor)
    val chipOutline = heimColorOrNull(component.borderColor)
    val chipShape = component.cornerRadius?.let { RoundedCornerShape(it.dp) }
        ?: FilterChipDefaults.shape
    val chipBorder = chipOutline?.let { BorderStroke((component.borderWidth ?: 1).dp, it) }

    when (component.variant) {
        // A filter chip announces itself as selected or not; an assist chip announces an action.
        // Using one for the other is the difference between a screen reader saying "selected" and
        // saying nothing at all.
        ChipVariant.FILTER -> FilterChip(
            selected = isSelected,
            onClick = onClick,
            label = label,
            enabled = component.isEnabled,
            leadingIcon = leadingIcon,
            modifier = chipModifier,
            shape = chipShape,
            border = chipBorder,
            colors = FilterChipDefaults.filterChipColors(
                containerColor = chipFill ?: Color.Transparent,
                labelColor = chipLabel ?: MaterialTheme.colorScheme.onSurfaceVariant,
                selectedContainerColor = chipSelected
                    ?: MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = chipSelected?.let { heimContentColorFor(it) }
                    ?: MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        )

        ChipVariant.ASSIST -> AssistChip(
            onClick = onClick,
            label = label,
            enabled = component.isEnabled,
            leadingIcon = leadingIcon,
            modifier = chipModifier,
            shape = chipShape,
            border = chipBorder,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = chipFill ?: Color.Transparent,
                labelColor = chipLabel ?: MaterialTheme.colorScheme.onSurface,
            ),
        )
    }
}
