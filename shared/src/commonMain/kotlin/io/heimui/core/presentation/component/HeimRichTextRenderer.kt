package io.heimui.core.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import io.heimui.core.domain.model.component.RichTextComponent
import io.heimui.core.presentation.accessibility.heimAccessibility
import io.heimui.core.presentation.designsystem.HeimTokenResolver
import io.heimui.core.presentation.designsystem.LocalHeimBrandTokens
import io.heimui.core.presentation.designsystem.heimColor
import io.heimui.core.presentation.launcher.LocalHeimUrlLauncher
import androidx.compose.ui.text.style.TextAlign as ComposeTextAlign
import io.heimui.core.domain.model.component.TextAlign as HeimTextAlign

/**
 * Renders a paragraph whose runs carry their own styling, and whose links go through the SDK's
 * URL policy rather than straight to the platform.
 *
 * That last part is the reason this is not simply `Text` with an `AnnotatedString`. A link inside
 * a sentence reaches the same launcher as an `open_url` action, so `intent://` and `file://` are
 * refused in a paragraph exactly as they are in a button. A payload cannot get a wider capability
 * by phrasing it as prose.
 */
@Composable
internal fun HeimRichTextRenderer(
    component: RichTextComponent,
    modifier: Modifier = Modifier,
) {
    val typography = MaterialTheme.typography
    val brandTokens = LocalHeimBrandTokens.current
    val urlLauncher = LocalHeimUrlLauncher.current
    val colorScheme = MaterialTheme.colorScheme

    val baseStyle = HeimTokenResolver.resolveTextStyle(component.style, typography, brandTokens)
    val baseColor = heimColor(component.color, colorScheme.onSurface)
    val linkColor = colorScheme.primary

    val annotated: AnnotatedString = buildAnnotatedString {
        component.spans.forEach { span ->
            val spanStyle = SpanStyle(
                // A span inherits the paragraph's style and overrides only what it names, so a
                // payload can bold three words without restating the size and family.
                color = span.color?.let { heimColor(it, baseColor) } ?: baseColor,
                fontWeight = span.weight?.toFontWeight(),
                fontSize = span.style
                    ?.let { HeimTokenResolver.resolveTextStyle(it, typography, brandTokens).fontSize }
                    ?: baseStyle.fontSize,
            )

            if (span.url != null) {
                withLink(
                    LinkAnnotation.Clickable(
                        tag = span.url,
                        styles = TextLinkStyles(
                            style = spanStyle.copy(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                            )
                        ),
                    ) { urlLauncher.openUrl(span.url) }
                ) {
                    append(span.text)
                }
            } else {
                withStyle(spanStyle) { append(span.text) }
            }
        }
    }

    Text(
        text = annotated,
        style = baseStyle.merge(LocalTextStyle.current.copy(color = baseColor)),
        textAlign = component.align.toCompose(),
        // 0 means unlimited, which is what a legal paragraph wants; Compose needs Int.MAX_VALUE.
        maxLines = component.maxLines.takeIf { it > 0 } ?: Int.MAX_VALUE,
        modifier = modifier
            .fillMaxWidth()
            .heimAccessibility(component.a11y, componentId = component.id),
    )
}

/** Accepts the CSS-ish names a design team writes, and the numeric weights a designer exports. */
private fun String.toFontWeight(): FontWeight? = when (trim().lowercase()) {
    "thin", "100" -> FontWeight.Thin
    "extralight", "200" -> FontWeight.ExtraLight
    "light", "300" -> FontWeight.Light
    "normal", "regular", "400" -> FontWeight.Normal
    "medium", "500" -> FontWeight.Medium
    "semibold", "600" -> FontWeight.SemiBold
    "bold", "700" -> FontWeight.Bold
    "extrabold", "800" -> FontWeight.ExtraBold
    "black", "900" -> FontWeight.Black
    else -> null
}

private fun HeimTextAlign.toCompose(): ComposeTextAlign = when (this) {
    HeimTextAlign.START -> ComposeTextAlign.Start
    HeimTextAlign.CENTER -> ComposeTextAlign.Center
    HeimTextAlign.END -> ComposeTextAlign.End
    HeimTextAlign.JUSTIFY -> ComposeTextAlign.Justify
}
