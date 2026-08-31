package io.heimui.core.presentation.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.withSign

object HeimBlurHashDecoder {

    fun decodeToPainter(blurHash: String?, width: Int = 32, height: Int = 32, punch: Float = 1f): Painter? {
        val colors = decode(blurHash, width, height, punch) ?: return null
        return BlurHashPainter(colors, width, height)
    }

    fun decode(blurHash: String?, width: Int = 32, height: Int = 32, punch: Float = 1f): List<Color>? {
        if (blurHash == null || blurHash.length < 6) return null

        return try {
            val numCompEnc = decode83(blurHash, 0, 1)
            val numCompX = (numCompEnc % 9) + 1
            val numCompY = (numCompEnc / 9) + 1

            val maxAcEnc = decode83(blurHash, 1, 2)
            val maxAc = (maxAcEnc + 1) / 166f

            val colors = Array(numCompX * numCompY) { FloatArray(3) }
            val averageColorEnc = decode83(blurHash, 2, 6)
            colors[0] = decodeDc(averageColorEnc)

            var i = 6
            for (count in 1 until numCompX * numCompY) {
                if (i + 2 <= blurHash.length) {
                    val acEnc = decode83(blurHash, i, i + 2)
                    colors[count] = decodeAc(acEnc, maxAc * punch)
                    i += 2
                }
            }

            val result = ArrayList<Color>(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    var r = 0f
                    var g = 0f
                    var b = 0f

                    for (j in 0 until numCompY) {
                        for (k in 0 until numCompX) {
                            val basis = (cos(PI * x * k / width) * cos(PI * y * j / height)).toFloat()
                            val color = colors[j * numCompX + k]
                            r += color[0] * basis
                            g += color[1] * basis
                            b += color[2] * basis
                        }
                    }

                    val intR = linearToSrgb(r)
                    val intG = linearToSrgb(g)
                    val intB = linearToSrgb(b)

                    result.add(Color(red = intR, green = intG, blue = intB))
                }
            }
            result
        } catch (_: Throwable) {
            null
        }
    }

    private class BlurHashPainter(
        private val colors: List<Color>,
        private val width: Int,
        private val height: Int
    ) : Painter() {
        override val intrinsicSize: Size = Size.Unspecified

        override fun DrawScope.onDraw() {
            val blockW = size.width / width
            val blockH = size.height / height

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val color = colors[y * width + x]
                    drawRect(
                        color = color,
                        topLeft = Offset(x * blockW, y * blockH),
                        size = Size(blockW + 0.5f, blockH + 0.5f)
                    )
                }
            }
        }
    }

    private fun decode83(str: String, from: Int, to: Int): Int {
        var result = 0
        for (i in from until to) {
            val c = str[i]
            val digit = charMap[c] ?: 0
            result = result * 83 + digit
        }
        return result
    }

    private fun decodeDc(colorEnc: Int): FloatArray {
        val r = colorEnc shr 16
        val g = (colorEnc shr 8) and 255
        val b = colorEnc and 255
        return floatArrayOf(srgbToLinear(r), srgbToLinear(g), srgbToLinear(b))
    }

    private fun decodeAc(value: Int, maxAc: Float): FloatArray {
        val r = value / (19 * 19)
        val g = (value / 19) % 19
        val b = value % 19
        return floatArrayOf(
            signedPow2((r - 9) / 9f) * maxAc,
            signedPow2((g - 9) / 9f) * maxAc,
            signedPow2((b - 9) / 9f) * maxAc
        )
    }

    private fun srgbToLinear(value: Int): Float {
        val v = value / 255f
        return if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
    }

    private fun linearToSrgb(value: Float): Int {
        val v = value.coerceIn(0f, 1f)
        val srgb = if (v <= 0.0031308f) v * 12.92f else 1.055f * v.pow(1 / 2.4f) - 0.055f
        return (srgb * 255 + 0.5f).toInt().coerceIn(0, 255)
    }

    private fun signedPow2(value: Float): Float {
        return value.pow(2).withSign(value)
    }

    private val charMap = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#$%*+,-.:;=?@[]^_{|}~"
        .mapIndexed { index, c -> c to index }
        .toMap()
}
