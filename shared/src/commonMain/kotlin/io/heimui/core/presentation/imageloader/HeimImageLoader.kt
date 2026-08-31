package io.heimui.core.presentation.imageloader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * Contract for rendering remote images in HeimUI.
 * Allows host applications to plug in alternative engines (Glide, Kamel, SDWebImage, or custom cache).
 */
interface HeimImageLoader {
    @Composable
    fun RenderImage(
        url: String,
        contentDescription: String?,
        blurHash: String?,
        cornerRadius: Int,
        height: Int?,
        aspectRatio: Float?,
        contentScale: ContentScale,
        modifier: Modifier
    )
}

val LocalHeimImageLoader = staticCompositionLocalOf<HeimImageLoader> {
    CoilHeimImageLoader()
}
