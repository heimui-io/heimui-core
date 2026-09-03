package io.heimui.core.presentation.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.heimui.core.presentation.designsystem.HeimTokenResolver
import io.heimui.core.presentation.designsystem.LocalHeimBrandTokens

// Shared plumbing for the style overrides an interactive control accepts. A payload names a
// colour rather than stating one, and every state a control needs -- pressed, disabled, selected
// -- is derived from that one name, so an author cannot leave a control half-specified.

/** Resolves a token, a Material role or a hex, or null when the payload said nothing. */
@Composable
@ReadOnlyComposable
internal fun heimColorOrNull(token: String?): Color? {
    if (token.isNullOrBlank()) return null
    val resolved = HeimTokenResolver.resolveColor(
        tokenOrHex = token,
        colorScheme = MaterialTheme.colorScheme,
        default = Color.Unspecified,
        brandTokens = LocalHeimBrandTokens.current,
    )
    // Unspecified means nothing here could resolve it: the app never registered that name. Falling
    // through to the variant's own colour is the honest answer, and the Studio warns about it at
    // authoring time where somebody can still fix it.
    return resolved.takeIf { it != Color.Unspecified }
}

/**
 * A readable foreground for an arbitrary background.
 *
 * An author who names only a fill still gets a label that can be read on it, in either theme,
 * without having to name the pair.
 */
internal fun heimContentColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color.Black else Color.White

/**
 * The colours of an outlined field, shared by `text_field`, `select` and `date_picker`.
 *
 * They look the same to a user and are the same Material component underneath, so styling them
 * three different ways would be three chances to drift.
 */
@Composable
internal fun heimOutlinedFieldColors(
    fill: Color?,
    text: Color?,
    outline: Color?,
    accent: Color?,
): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = text ?: MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = text ?: MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = fill ?: Color.Transparent,
    unfocusedContainerColor = fill ?: Color.Transparent,
    // Focus is what `accent_color` means on a field: the outline is how it says where the caret is.
    focusedBorderColor = accent ?: outline ?: MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = outline ?: MaterialTheme.colorScheme.outline,
    focusedLabelColor = accent ?: MaterialTheme.colorScheme.primary,
    cursorColor = accent ?: MaterialTheme.colorScheme.primary,
)

/** Material's disabled opacities, so a control given a colour still greys out like a control. */
internal object HeimDisabledAlpha {
    const val CONTAINER: Float = 0.12f
    const val CONTENT: Float = 0.38f
}
