package io.heimui.core.presentation.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import io.heimui.core.domain.model.component.Alignment as HeimAlignment
import io.heimui.core.domain.model.component.ContentScale as HeimContentScale
import io.heimui.core.domain.model.component.TextAlign as HeimTextAlign

public object HeimTokenResolver {

    public fun resolveColor(
        tokenOrHex: String?,
        colorScheme: ColorScheme,
        default: Color = Color.Unspecified,
        brandTokens: HeimBrandTokens = HeimBrandTokens.default
    ): Color {
        if (tokenOrHex.isNullOrBlank()) return default

        val clean = tokenOrHex.trim()

        // 1. Brand Tokens override
        val customColor = brandTokens.getColor(clean)
        if (customColor != null) return customColor

        // 2. Hex Color parsing
        if (clean.startsWith("#")) {
            return parseHexColor(clean) ?: default
        }

        // 3. Material 3 Semantic Color Tokens
        return when (clean.lowercase()) {
            "primary" -> colorScheme.primary
            "onprimary" -> colorScheme.onPrimary
            "primarycontainer" -> colorScheme.primaryContainer
            "onprimarycontainer" -> colorScheme.onPrimaryContainer
            "inverseprimary" -> colorScheme.inversePrimary

            "secondary" -> colorScheme.secondary
            "onsecondary" -> colorScheme.onSecondary
            "secondarycontainer" -> colorScheme.secondaryContainer
            "onsecondarycontainer" -> colorScheme.onSecondaryContainer

            "tertiary" -> colorScheme.tertiary
            "ontertiary" -> colorScheme.onTertiary
            "tertiarycontainer" -> colorScheme.tertiaryContainer
            "ontertiarycontainer" -> colorScheme.onTertiaryContainer

            "background" -> colorScheme.background
            "onbackground" -> colorScheme.onBackground

            "surface" -> colorScheme.surface
            "onsurface" -> colorScheme.onSurface
            "surfacevariant" -> colorScheme.surfaceVariant
            "onsurfacevariant" -> colorScheme.onSurfaceVariant
            "inversesurface" -> colorScheme.inverseSurface
            "inverseonsurface" -> colorScheme.inverseOnSurface
            "surfacetint" -> colorScheme.surfaceTint

            "outline" -> colorScheme.outline
            "outlinevariant" -> colorScheme.outlineVariant
            "scrim" -> colorScheme.scrim

            "error" -> colorScheme.error
            "onerror" -> colorScheme.onError
            "errorcontainer" -> colorScheme.errorContainer
            "onerrorcontainer" -> colorScheme.onErrorContainer

            "transparent" -> Color.Transparent
            "white" -> Color.White
            "black" -> Color.Black

            else -> default
        }
    }

    public fun parseHexColor(hex: String): Color? {
        val clean = hex.removePrefix("#").trim()
        return try {
            when (clean.length) {
                3 -> { // #RGB -> #RRGGBB
                    val r = clean[0].toString().repeat(2).toInt(16)
                    val g = clean[1].toString().repeat(2).toInt(16)
                    val b = clean[2].toString().repeat(2).toInt(16)
                    Color(r, g, b, 255)
                }
                4 -> { // #RGBA
                    val r = clean[0].toString().repeat(2).toInt(16)
                    val g = clean[1].toString().repeat(2).toInt(16)
                    val b = clean[2].toString().repeat(2).toInt(16)
                    val a = clean[3].toString().repeat(2).toInt(16)
                    Color(r, g, b, a)
                }
                6 -> { // #RRGGBB
                    val r = clean.substring(0, 2).toInt(16)
                    val g = clean.substring(2, 4).toInt(16)
                    val b = clean.substring(4, 6).toInt(16)
                    Color(r, g, b, 255)
                }
                8 -> { // #AARRGGBB
                    val a = clean.substring(0, 2).toInt(16)
                    val r = clean.substring(2, 4).toInt(16)
                    val g = clean.substring(4, 6).toInt(16)
                    val b = clean.substring(6, 8).toInt(16)
                    Color(r, g, b, a)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    public fun resolveTextStyle(
        styleName: String?,
        typography: Typography,
        brandTokens: HeimBrandTokens = HeimBrandTokens.default
    ): TextStyle {
        if (styleName.isNullOrBlank()) return typography.bodyMedium
        val clean = styleName.trim()

        val customStyle = brandTokens.getTextStyle(clean)
        if (customStyle != null) return customStyle

        return when (clean.lowercase()) {
            "displaylarge" -> typography.displayLarge
            "displaymedium" -> typography.displayMedium
            "displaysmall" -> typography.displaySmall

            "headlinelarge" -> typography.headlineLarge
            "headlinemedium" -> typography.headlineMedium
            "headlinesmall" -> typography.headlineSmall

            "titlelarge" -> typography.titleLarge
            "titlemedium" -> typography.titleMedium
            "titlesmall" -> typography.titleSmall

            "bodylarge" -> typography.bodyLarge
            "bodysmall" -> typography.bodySmall
            "bodymedium" -> typography.bodyMedium

            "labellarge" -> typography.labelLarge
            "labelmedium" -> typography.labelMedium
            "labelsmall" -> typography.labelSmall

            else -> typography.bodyMedium
        }
    }

    public fun resolveHorizontalAlignment(alignment: HeimAlignment): Alignment.Horizontal {
        return when (alignment) {
            HeimAlignment.START -> Alignment.Start
            HeimAlignment.CENTER -> Alignment.CenterHorizontally
            HeimAlignment.END -> Alignment.End
            else -> Alignment.Start
        }
    }

    public fun resolveVerticalAlignment(alignment: HeimAlignment): Alignment.Vertical {
        return when (alignment) {
            HeimAlignment.TOP, HeimAlignment.START -> Alignment.Top
            HeimAlignment.CENTER -> Alignment.CenterVertically
            HeimAlignment.BOTTOM, HeimAlignment.END -> Alignment.Bottom
        }
    }

    public fun resolveBoxAlignment(alignment: HeimAlignment): Alignment {
        return when (alignment) {
            HeimAlignment.START -> Alignment.CenterStart
            HeimAlignment.CENTER -> Alignment.Center
            HeimAlignment.END -> Alignment.CenterEnd
            HeimAlignment.TOP -> Alignment.TopCenter
            HeimAlignment.BOTTOM -> Alignment.BottomCenter
        }
    }

    public fun resolveTextAlign(textAlign: HeimTextAlign): TextAlign {
        return when (textAlign) {
            HeimTextAlign.START -> TextAlign.Start
            HeimTextAlign.CENTER -> TextAlign.Center
            HeimTextAlign.END -> TextAlign.End
            HeimTextAlign.JUSTIFY -> TextAlign.Justify
        }
    }

    public fun resolveContentScale(scale: HeimContentScale): ContentScale {
        return when (scale) {
            HeimContentScale.CROP -> ContentScale.Crop
            HeimContentScale.FIT -> ContentScale.Fit
            HeimContentScale.FILL_BOUNDS -> ContentScale.FillBounds
            HeimContentScale.INSIDE -> ContentScale.Inside
        }
    }
}
