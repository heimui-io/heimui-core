package io.heimui.core.presentation.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HeimSkeletonRenderer(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    val shimmerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Bar Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerColor)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
            )
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerColor)
            )
        }

        // Hero Card Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmerColor)
        )

        // Text Line Skeletons
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerColor)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerColor)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Input Fields Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerColor)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerColor)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Action Button Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(shimmerColor)
        )
    }
}
