package io.heimui.core.presentation.accessibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import io.heimui.core.domain.model.accessibility.AccessibilityRole
import io.heimui.core.domain.model.accessibility.HeimAccessibility

/**
 * Applies the payload's accessibility semantics and, when given, a stable [testTag] derived from
 * the component id -- without it a host has no way to address SDUI-rendered nodes from a UI test.
 */
public fun Modifier.heimAccessibility(
    a11y: HeimAccessibility?,
    mergeDescendants: Boolean = false,
    componentId: String? = null
): Modifier {
    val tagged = if (componentId.isNullOrBlank()) this else this.testTag(componentId)
    if (a11y == null) return tagged

    return tagged.semantics(mergeDescendants = mergeDescendants) {
        if (a11y.hiddenFromAccessibility) {
            hideFromAccessibility()
            return@semantics
        }

        a11y.contentDescription?.let {
            contentDescription = it
        }

        if (a11y.isHeading || a11y.role == AccessibilityRole.HEADER) {
            heading()
        }

        a11y.stateDescription?.let {
            stateDescription = it
        }

        a11y.role?.let { r ->
            when (r) {
                AccessibilityRole.BUTTON -> role = Role.Button
                AccessibilityRole.IMAGE -> role = Role.Image
                AccessibilityRole.SWITCH -> role = Role.Switch
                AccessibilityRole.TAB -> role = Role.Tab
                AccessibilityRole.HEADER -> heading()
            }
        }
    }
}
