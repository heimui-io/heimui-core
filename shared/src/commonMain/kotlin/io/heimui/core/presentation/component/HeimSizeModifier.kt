package io.heimui.core.presentation.component

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.heimui.core.domain.model.component.HeimSize

/**
 * Applies a component's requested dimensions.
 *
 * Order matters. A fixed dimension is applied instead of the minimum on the same axis, because
 * asking for both is contradictory and the exact measurement is the more specific instruction.
 * `aspectRatio` comes last and only when exactly one axis is constrained — it exists to derive the
 * other one, so with both fixed it has nothing to compute, and with neither it has nothing to
 * derive from.
 */
internal fun Modifier.heimSize(size: HeimSize): Modifier {
    if (size.isEmpty) return this

    var modifier = this
    when {
        size.width != null -> modifier = modifier.width(size.width.dp)
        size.minWidth != null -> modifier = modifier.widthIn(min = size.minWidth.dp)
    }
    when {
        size.height != null -> modifier = modifier.height(size.height.dp)
        size.minHeight != null -> modifier = modifier.heightIn(min = size.minHeight.dp)
    }

    val horizontallyFixed = size.width != null
    val verticallyFixed = size.height != null
    if (size.aspectRatio != null && horizontallyFixed != verticallyFixed) {
        modifier = modifier.aspectRatio(size.aspectRatio)
    }
    return modifier
}
