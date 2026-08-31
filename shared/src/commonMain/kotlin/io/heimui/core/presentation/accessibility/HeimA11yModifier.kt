package io.heimui.core.presentation.accessibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import io.heimui.core.domain.model.accessibility.AccessibilityRole
import io.heimui.core.domain.model.accessibility.HeimAccessibility

fun Modifier.heimAccessibility(a11y: HeimAccessibility?): Modifier {
    if (a11y == null) return this

    return this.semantics(mergeDescendants = true) {
        if (a11y.hiddenFromAccessibility) {
            hideFromAccessibility()
            return@semantics
        }

        a11y.contentDescription?.let {
            contentDescription = it
        }

        if (a11y.isHeading) {
            heading()
        }

        a11y.stateDescription?.let {
            stateDescription = it
        }

        a11y.role?.let { r ->
            role = when (r) {
                AccessibilityRole.BUTTON -> Role.Button
                AccessibilityRole.IMAGE -> Role.Image
                AccessibilityRole.SWITCH -> Role.Switch
                AccessibilityRole.TAB -> Role.Tab
                AccessibilityRole.HEADER -> Role.Button
            }
        }
    }
}
