package io.heimui.core.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.component.BadgeComponent
import io.heimui.core.domain.model.component.CardComponent
import io.heimui.core.domain.model.component.IconComponent
import io.heimui.core.domain.model.component.ImageComponent
import io.heimui.core.domain.model.component.TextComponent
import io.heimui.core.presentation.HeimRenderer
import io.heimui.core.presentation.accessibility.heimAccessibility
import io.heimui.core.presentation.designsystem.HeimTokenResolver
import io.heimui.core.presentation.designsystem.heimColor
import io.heimui.core.presentation.designsystem.heimTextStyle
import io.heimui.core.presentation.designsystem.LocalHeimIconProvider
import io.heimui.core.presentation.imageloader.LocalHeimImageLoader
import io.heimui.core.presentation.action.LocalHeimActionRunner
import io.heimui.core.presentation.state.HeimStateManager

@Composable
internal fun HeimTextRenderer(
    component: TextComponent,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val textColor = heimColor(component.color, colorScheme.onSurface)
    val textStyle = heimTextStyle(component.style)
    val textAlign = HeimTokenResolver.resolveTextAlign(component.textAlign)

    Text(
        text = component.text,
        color = textColor,
        style = textStyle,
        textAlign = textAlign,
        maxLines = component.maxLines ?: Int.MAX_VALUE,
        overflow = if (component.maxLines != null) TextOverflow.Ellipsis else TextOverflow.Clip,
        modifier = modifier.heimAccessibility(component.a11y, componentId = component.id)
    )
}

@Composable
internal fun HeimImageRenderer(
    component: ImageComponent,
    modifier: Modifier = Modifier
) {
    val imageLoader = LocalHeimImageLoader.current
    val contentScale = HeimTokenResolver.resolveContentScale(component.contentScale)

    imageLoader.RenderImage(
        url = component.url,
        contentDescription = component.a11y?.contentDescription,
        blurHash = component.blurHash,
        cornerRadius = component.cornerRadius,
        height = component.height,
        aspectRatio = component.aspectRatio,
        contentScale = contentScale,
        modifier = modifier.heimAccessibility(component.a11y, componentId = component.id)
    )
}

@Composable
internal fun HeimCardRenderer(
    component: CardComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = heimColor(component.backgroundColor, colorScheme.surface)
    val borderColor = heimColor(component.borderColor, Color.Transparent)

    val shape = RoundedCornerShape(component.cornerRadius.dp)
    // The width was hardcoded, so a card could say what colour its border is and not how thick --
    // the one bordered surface that could not, while a `box` beside it could.
    val borderStroke = if (component.borderColor != null) {
        BorderStroke(component.borderWidth.dp, borderColor)
    } else {
        null
    }

    var cardModifier = modifier
        .heimFillWidth(component)
        .heimAccessibility(component.a11y, componentId = component.id)

    val actionRunner = LocalHeimActionRunner.current
    if (component.actions.isNotEmpty()) {
        // The runner, not a forEach: a card whose actions submit and then navigate must do
        // those in that order, and only navigate if the submission worked.
        cardModifier = cardModifier.clickable { actionRunner.run(component.actions) }
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = component.elevation.dp),
        border = borderStroke
    ) {
        Box(modifier = Modifier.heimPadding(component.padding)) {
            HeimRenderer(
                component = component.child,
                stateManager = stateManager,
                onAction = onAction
            )
        }
    }
}

@Composable
internal fun HeimBadgeRenderer(
    component: BadgeComponent,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = heimColor(component.backgroundColor, colorScheme.primaryContainer)
    val textColor = heimColor(component.textColor, colorScheme.onPrimaryContainer)

    Surface(
        modifier = modifier.heimAccessibility(component.a11y, componentId = component.id),
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = component.text,
                color = textColor,
                // No hardcoded fontSize: it silently overrode the design-system token.
                style = MaterialTheme.typography.labelSmall,
                // A badge is a pill holding "NEW", "3" or "PREMIUM". Two lines of it is never
                // what anybody meant, and the default here was Compose's for running text.
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun HeimIconRenderer(
    component: IconComponent,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val tint = heimColor(component.tint, colorScheme.onSurface)

    val iconProvider = LocalHeimIconProvider.current
    iconProvider.RenderIcon(
        name = component.name,
        tint = tint,
        size = component.size.dp,
        modifier = modifier.heimAccessibility(component.a11y, componentId = component.id)
    )
}
