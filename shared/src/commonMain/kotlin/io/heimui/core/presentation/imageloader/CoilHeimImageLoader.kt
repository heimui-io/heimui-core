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
import io.ktor.http.Url
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
    private val allowedSchemes: Set<String> = setOf("https", "data"),
    /**
     * Hosts whose images may be served over cleartext `http://`.
     *
     * Empty by default, and populated from `HeimConfig.allowCleartextHosts` by
     * [io.heimui.core.presentation.designsystem.HeimTheme]. Adding `http` to [allowedSchemes]
     * instead would open cleartext to every host a payload can name, which is the one thing the
     * scheme allowlist is there to prevent.
     */
    private val cleartextHosts: Set<String> = emptySet()
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

        if (!isAllowed(url)) return

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

    /**
     * Whether this URL may be loaded at all.
     *
     * `http` is not a scheme the loader accepts outright — it is accepted for the hosts the app
     * declared as cleartext, so a local development backend works without every payload on every
     * other host gaining the same licence.
     */
    private fun isAllowed(url: String): Boolean {
        // Empty means relative, which the loader resolves against nothing dangerous.
        val scheme = url.substringBefore(':', "").lowercase()
        if (scheme.isEmpty() || scheme in allowedSchemes) return true
        if (scheme != "http" || cleartextHosts.isEmpty()) return false
        val host = runCatching { Url(url).host }.getOrNull() ?: return false
        return cleartextHosts.any { it.equals(host, ignoreCase = true) }
    }
}
