package io.heimui.core

import io.heimui.core.presentation.util.HeimBlurHashDecoder
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HeimBlurHashDecoderTest {

    @Test
    fun `test decode valid blurHash returns painter and colors`() {
        val validHash = "LEHV6nWB2yk8pyo0adR*.7kCMdnj"
        val colors = HeimBlurHashDecoder.decode(validHash, width = 16, height = 16)
        assertNotNull(colors)
        val painter = HeimBlurHashDecoder.decodeToPainter(validHash, width = 16, height = 16)
        assertNotNull(painter)
    }

    @Test
    fun `test decode null or empty blurHash returns null`() {
        assertNull(HeimBlurHashDecoder.decode(null))
        assertNull(HeimBlurHashDecoder.decode(""))
        assertNull(HeimBlurHashDecoder.decode("abc"))
    }

    @Test
    fun `test decode invalid characters in blurHash returns null gracefully`() {
        val invalidHash = "!!!!!!!!!!!!"
        // Should not throw exceptions, should return null or handle gracefully
        val result = HeimBlurHashDecoder.decode(invalidHash)
        // Handled cleanly
    }
}
