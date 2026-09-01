package io.heimui.core.presentation.designsystem

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Enterprise Brand Design Tokens registry.
 * Allows host apps to register custom brand semantic colors, gradients, and custom text styles.
 */
public class HeimBrandTokens(
    private val colors: Map<String, Color> = emptyMap(),
    private val textStyles: Map<String, TextStyle> = emptyMap()
) {
    public fun getColor(token: String): Color? = colors[token.lowercase()]
    public fun getTextStyle(token: String): TextStyle? = textStyles[token.lowercase()]

    public companion object {
        public val default: HeimBrandTokens = HeimBrandTokens()

        public fun build(builder: Builder.() -> Unit): HeimBrandTokens {
            return Builder().apply(builder).build()
        }
    }

    public class Builder {
        private val colors = mutableMapOf<String, Color>()
        private val textStyles = mutableMapOf<String, TextStyle>()

        public fun color(token: String, color: Color): Builder = apply {
            colors[token.lowercase()] = color
        }

        public fun textStyle(token: String, style: TextStyle): Builder = apply {
            textStyles[token.lowercase()] = style
        }

        public fun build(): HeimBrandTokens = HeimBrandTokens(colors.toMap(), textStyles.toMap())
    }
}

public val LocalHeimBrandTokens: ProvidableCompositionLocal<HeimBrandTokens> =
    staticCompositionLocalOf {
    HeimBrandTokens.default
}
