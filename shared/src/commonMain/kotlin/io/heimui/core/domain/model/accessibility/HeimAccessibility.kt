package io.heimui.core.domain.model.accessibility

public data class HeimAccessibility(
    val contentDescription: String? = null,
    val role: AccessibilityRole? = null,
    val isHeading: Boolean = false,
    val stateDescription: String? = null,
    val hiddenFromAccessibility: Boolean = false
)

public enum class AccessibilityRole {
    BUTTON,
    IMAGE,
    HEADER,
    SWITCH,
    TAB
}
