package io.heimui.core.presentation.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Composable accessors that resolve design tokens against the brand registry installed by
 * [HeimTheme].
 *
 * Renderers previously called `HeimTokenResolver.resolveColor(token, colorScheme, default)` and
 * omitted the `brandTokens` argument, so it always fell back to the empty
 * `HeimBrandTokens.default` singleton -- brand tokens registered by a host were never applied.
 * Reading the CompositionLocal here is what makes the registry actually reachable.
 */
public @Composable
@ReadOnlyComposable
fun heimColor(tokenOrHex: String?, default: Color): Color {
    val brand = LocalHeimBrandTokens.current
    val scheme = MaterialTheme.colorScheme
    return HeimTokenResolver.resolveColor(tokenOrHex, scheme, default, brand)
}

public @Composable
@ReadOnlyComposable
fun heimTextStyle(styleName: String?): TextStyle {
    val brand = LocalHeimBrandTokens.current
    return HeimTokenResolver.resolveTextStyle(styleName, MaterialTheme.typography, brand)
}
