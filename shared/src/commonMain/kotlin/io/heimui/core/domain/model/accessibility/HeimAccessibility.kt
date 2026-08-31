package io.heimui.core.domain.model.accessibility

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HeimAccessibility(
    @SerialName("content_description") val contentDescription: String? = null,
    val role: AccessibilityRole? = null,
    val isHeading: Boolean = false,
    val stateDescription: String? = null,
    val hiddenFromAccessibility: Boolean = false
)

@Serializable
enum class AccessibilityRole {
    @SerialName("BUTTON") BUTTON,
    @SerialName("IMAGE") IMAGE,
    @SerialName("HEADER") HEADER,
    @SerialName("SWITCH") SWITCH,
    @SerialName("TAB") TAB
}
