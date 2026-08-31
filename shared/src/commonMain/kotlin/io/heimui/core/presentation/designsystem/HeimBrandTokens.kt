package io.heimui.core.presentation.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Enterprise Brand Design Tokens registry.
 * Allows host apps to register custom brand semantic colors, gradients, and custom text styles.
 */
class HeimBrandTokens(
    private val colors: Map<String, Color> = emptyMap(),
    private val textStyles: Map<String, TextStyle> = emptyMap()
) {
    fun getColor(token: String): Color? = colors[token.lowercase()]
    fun getTextStyle(token: String): TextStyle? = textStyles[token.lowercase()]

    companion object {
        val default = HeimBrandTokens()

        fun build(builder: Builder.() -> Unit): HeimBrandTokens {
            return Builder().apply(builder).build()
        }
    }

    class Builder {
        private val colors = mutableMapOf<String, Color>()
        private val textStyles = mutableMapOf<String, TextStyle>()

        fun color(token: String, color: Color) = apply {
            colors[token.lowercase()] = color
        }

        fun textStyle(token: String, style: TextStyle) = apply {
            textStyles[token.lowercase()] = style
        }

        fun build() = HeimBrandTokens(colors.toMap(), textStyles.toMap())
    }
}

val LocalHeimBrandTokens = staticCompositionLocalOf<HeimBrandTokens> {
    HeimBrandTokens.default
}
