package io.heimui.core.presentation.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.heimui.core.domain.model.component.HeimPadding

/**
 * Turns a payload's padding into layout padding.
 *
 * Nothing is applied when every side is zero, so the common case adds no modifier node to the
 * tree at all.
 */
internal fun Modifier.heimPadding(padding: HeimPadding): Modifier =
    if (padding.isEmpty) this else padding(padding.toPaddingValues())

/**
 * `start`/`end` rather than left/right, so the same payload mirrors correctly in a right-to-left
 * locale without the server knowing which locale the reader is in.
 */
internal fun HeimPadding.toPaddingValues(): PaddingValues = PaddingValues(
    start = start.dp,
    top = top.dp,
    end = end.dp,
    bottom = bottom.dp,
)
