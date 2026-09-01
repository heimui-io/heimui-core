package io.heimui.core.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.sp
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.component.BadgeComponent
import io.heimui.core.domain.model.component.CardComponent
import io.heimui.core.domain.model.component.IconComponent
import io.heimui.core.domain.model.component.ImageComponent
import io.heimui.core.domain.model.component.TextComponent
import io.heimui.core.presentation.HeimRenderer
import io.heimui.core.presentation.accessibility.heimAccessibility
import io.heimui.core.presentation.designsystem.HeimTokenResolver
import io.heimui.core.presentation.designsystem.LocalHeimIconProvider
import io.heimui.core.presentation.imageloader.LocalHeimImageLoader
import io.heimui.core.presentation.state.HeimStateManager

@Composable
fun HeimTextRenderer(
    component: TextComponent,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val textColor = HeimTokenResolver.resolveColor(
        tokenOrHex = component.color,
        colorScheme = colorScheme,
        default = colorScheme.onSurface
    )
    val textStyle = HeimTokenResolver.resolveTextStyle(
        styleName = component.style,
        typography = typography
    )
    val textAlign = HeimTokenResolver.resolveTextAlign(component.textAlign)

    Text(
        text = component.text,
        color = textColor,
        style = textStyle,
        textAlign = textAlign,
        maxLines = component.maxLines ?: Int.MAX_VALUE,
        overflow = if (component.maxLines != null) TextOverflow.Ellipsis else TextOverflow.Clip,
        modifier = modifier.heimAccessibility(component.a11y)
    )
}

@Composable
fun HeimImageRenderer(
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
        modifier = modifier.heimAccessibility(component.a11y)
    )
}

@Composable
fun HeimCardRenderer(
    component: CardComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = HeimTokenResolver.resolveColor(
        component.backgroundColor,
        colorScheme,
        colorScheme.surface
    )
    val borderColor = HeimTokenResolver.resolveColor(
        component.borderColor,
        colorScheme,
        Color.Transparent
    )

    val shape = RoundedCornerShape(component.cornerRadius.dp)
    val borderStroke = if (component.borderColor != null) BorderStroke(1.dp, borderColor) else null

    var cardModifier = modifier
        .fillMaxWidth()
        .heimAccessibility(component.a11y)

    if (component.actions.isNotEmpty()) {
        cardModifier = cardModifier.clickable {
            component.actions.forEach { action ->
                onAction(action)
            }
        }
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = component.elevation.dp),
        border = borderStroke
    ) {
        Box(modifier = Modifier.padding(component.padding.dp)) {
            HeimRenderer(
                component = component.child,
                stateManager = stateManager,
                onAction = onAction
            )
        }
    }
}

@Composable
fun HeimBadgeRenderer(
    component: BadgeComponent,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = HeimTokenResolver.resolveColor(
        component.backgroundColor,
        colorScheme,
        colorScheme.primaryContainer
    )
    val textColor = HeimTokenResolver.resolveColor(
        component.textColor,
        colorScheme,
        colorScheme.onPrimaryContainer
    )

    Surface(
        modifier = modifier.heimAccessibility(component.a11y),
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
                fontSize = 12.sp,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun HeimIconRenderer(
    component: IconComponent,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val tint = HeimTokenResolver.resolveColor(
        component.tint,
        colorScheme,
        colorScheme.onSurface
    )

    val iconProvider = LocalHeimIconProvider.current
    iconProvider.RenderIcon(
        name = component.name,
        tint = tint,
        size = component.size.dp,
        modifier = modifier.heimAccessibility(component.a11y)
    )
}
