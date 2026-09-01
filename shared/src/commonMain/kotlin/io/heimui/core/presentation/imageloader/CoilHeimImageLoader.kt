package io.heimui.core.presentation.imageloader

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.heimui.core.presentation.util.HeimBlurHashDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Default [HeimImageLoader] powered by Coil 3, with BlurHash placeholders.
 */
public class CoilHeimImageLoader(
    private val blurWidth: Int = 24,
    private val blurHeight: Int = 24,
    private val blurPunch: Float = 1f,
    /** Schemes an image URL may use. `file://` and `content://` would expose local storage. */
    private val allowedSchemes: Set<String> = setOf("https", "data")
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
        val shape = RoundedCornerShape(cornerRadius.coerceAtLeast(0).dp)
        var imageModifier = modifier
            .fillMaxWidth()
            .clip(shape)

        imageModifier = when {
            height != null && height > 0 -> imageModifier.height(height.dp)
            aspectRatio != null && aspectRatio > 0f -> imageModifier.aspectRatio(aspectRatio)
            else -> imageModifier.height(180.dp)
        }

        // Decoding a BlurHash is O(w*h*compX*compY); doing it inside remember{} ran ~83k cos()
        // calls on the UI thread for every item appearing in a list.
        val placeholder: Painter? by produceState<Painter?>(null, blurHash, blurWidth, blurHeight) {
            value = if (blurHash == null) {
                null
            } else {
                withContext(Dispatchers.Default) {
                    HeimBlurHashDecoder
                        .decodeToImageBitmap(blurHash, blurWidth, blurHeight, blurPunch)
                        ?.let { BitmapPainter(it) }
                }
            }
        }

        val scheme = url.substringBefore(':', "").lowercase()
        if (scheme.isNotEmpty() && scheme !in allowedSchemes) return

        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            placeholder = placeholder,
            // Deliberately not the same painter as the placeholder: a permanently failed image
            // must be distinguishable from one that is still loading.
            error = null,
            contentScale = contentScale,
            modifier = imageModifier
        )
    }
}
