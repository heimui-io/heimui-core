package io.heimui.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class HeimAccessibilityDto(
    @SerialName("content_description") val contentDescription: String? = null,
    val role: AccessibilityRoleDto? = null,
    @SerialName("is_heading") val isHeading: Boolean = false,
    @SerialName("state_description") val stateDescription: String? = null,
    @SerialName("hidden_from_accessibility") val hiddenFromAccessibility: Boolean = false
)

@Serializable
public enum class AccessibilityRoleDto {
    @SerialName("BUTTON") BUTTON,
    @SerialName("IMAGE") IMAGE,
    @SerialName("HEADER") HEADER,
    @SerialName("SWITCH") SWITCH,
    @SerialName("TAB") TAB
}
