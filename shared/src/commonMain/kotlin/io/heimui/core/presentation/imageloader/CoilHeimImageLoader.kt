package io.heimui.core.presentation.imageloader

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.heimui.core.presentation.util.HeimBlurHashDecoder

/**
 * Default implementation of [HeimImageLoader] powered by Coil 3 and BlurHash.
 */
class CoilHeimImageLoader(
    private val blurWidth: Int = 32,
    private val blurHeight: Int = 32,
    private val blurPunch: Float = 1f
) : HeimImageLoader {

    @Composable
    override fun RenderImage(
        url: String,
        contentDescription: String?,
        blurHash: String?,
        cornerRadius: Int,
        height: Int?,
        aspectRatio: Float?,
        contentScale: ContentScale,
        modifier: Modifier
    ) {
        val shape = RoundedCornerShape(cornerRadius.dp)
        var imageModifier = modifier
            .fillMaxWidth()
            .clip(shape)

        if (height != null) {
            imageModifier = imageModifier.height(height.dp)
        } else if (aspectRatio != null && aspectRatio > 0) {
            imageModifier = imageModifier.aspectRatio(aspectRatio)
        } else {
            imageModifier = imageModifier.height(180.dp)
        }

        val placeholderPainter = remember(blurHash) {
            HeimBlurHashDecoder.decodeToPainter(
                blurHash = blurHash,
                width = blurWidth,
                height = blurHeight,
                punch = blurPunch
            )
        }

        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            contentScale = contentScale,
            modifier = imageModifier
        )
    }
}
