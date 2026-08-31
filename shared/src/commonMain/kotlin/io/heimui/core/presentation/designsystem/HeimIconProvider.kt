package io.heimui.core.presentation.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun interface HeimIconProvider {
    @Composable
    fun RenderIcon(name: String, tint: Color, size: Dp, modifier: Modifier)
}

val LocalHeimIconProvider = staticCompositionLocalOf<HeimIconProvider> {
    DefaultHeimIconProvider
}

object DefaultHeimIconProvider : HeimIconProvider {

    @Composable
    override fun RenderIcon(name: String, tint: Color, size: Dp, modifier: Modifier) {
        val cleanName = name.lowercase().trim()

        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                when (cleanName) {
                    "check", "done" -> drawCheck(tint)
                    "close", "clear" -> drawClose(tint)
                    "star" -> drawStar(tint)
                    "favorite", "heart" -> drawHeart(tint)
                    "search" -> drawSearch(tint)
                    "add", "plus" -> drawPlus(tint)
                    "arrow_back", "back", "chevron_left" -> drawArrowBack(tint)
                    "arrow_forward", "forward", "chevron_right" -> drawArrowForward(tint)
                    "info" -> drawInfo(tint)
                    "settings" -> drawSettings(tint)
                    "person", "user" -> drawPerson(tint)
                    "home" -> drawHome(tint)
                    else -> drawInfo(tint)
                }
            }
        }
    }

    private fun DrawScope.drawCheck(tint: Color) {
        val stroke = size.width * 0.12f
        val path = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.5f)
            lineTo(size.width * 0.42f, size.height * 0.72f)
            lineTo(size.width * 0.8f, size.height * 0.28f)
        }
        drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }

    private fun DrawScope.drawClose(tint: Color) {
        val stroke = size.width * 0.12f
        drawLine(tint, Offset(size.width * 0.25f, size.height * 0.25f), Offset(size.width * 0.75f, size.height * 0.75f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.75f, size.height * 0.25f), Offset(size.width * 0.25f, size.height * 0.75f), strokeWidth = stroke, cap = StrokeCap.Round)
    }

    private fun DrawScope.drawPlus(tint: Color) {
        val stroke = size.width * 0.12f
        drawLine(tint, Offset(size.width * 0.5f, size.height * 0.2f), Offset(size.width * 0.5f, size.height * 0.8f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.2f, size.height * 0.5f), Offset(size.width * 0.8f, size.height * 0.5f), strokeWidth = stroke, cap = StrokeCap.Round)
    }

    private fun DrawScope.drawStar(tint: Color) {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerRadius = size.width * 0.45f
        val innerRadius = outerRadius * 0.45f

        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val angle = (i * 36 - 90) * (PI / 180f)
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color = tint)
    }

    private fun DrawScope.drawHeart(tint: Color) {
        val path = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.8f)
            cubicTo(size.width * 0.15f, size.height * 0.55f, size.width * 0.1f, size.height * 0.25f, size.width * 0.32f, size.height * 0.25f)
            cubicTo(size.width * 0.45f, size.height * 0.25f, size.width * 0.5f, size.height * 0.35f, size.width * 0.5f, size.height * 0.35f)
            cubicTo(size.width * 0.5f, size.height * 0.35f, size.width * 0.55f, size.height * 0.25f, size.width * 0.68f, size.height * 0.25f)
            cubicTo(size.width * 0.9f, size.height * 0.25f, size.width * 0.85f, size.height * 0.55f, size.width * 0.5f, size.height * 0.8f)
            close()
        }
        drawPath(path, color = tint)
    }

    private fun DrawScope.drawSearch(tint: Color) {
        val stroke = size.width * 0.1f
        val r = size.width * 0.26f
        val cx = size.width * 0.42f
        val cy = size.height * 0.42f
        drawCircle(color = tint, radius = r, center = Offset(cx, cy), style = Stroke(width = stroke))
        drawLine(tint, Offset(cx + r * 0.7f, cy + r * 0.7f), Offset(size.width * 0.82f, size.height * 0.82f), strokeWidth = stroke, cap = StrokeCap.Round)
    }

    private fun DrawScope.drawArrowBack(tint: Color) {
        val stroke = size.width * 0.12f
        drawLine(tint, Offset(size.width * 0.25f, size.height * 0.5f), Offset(size.width * 0.75f, size.height * 0.5f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.25f, size.height * 0.5f), Offset(size.width * 0.5f, size.height * 0.25f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.25f, size.height * 0.5f), Offset(size.width * 0.5f, size.height * 0.75f), strokeWidth = stroke, cap = StrokeCap.Round)
    }

    private fun DrawScope.drawArrowForward(tint: Color) {
        val stroke = size.width * 0.12f
        drawLine(tint, Offset(size.width * 0.25f, size.height * 0.5f), Offset(size.width * 0.75f, size.height * 0.5f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.75f, size.height * 0.5f), Offset(size.width * 0.5f, size.height * 0.25f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.75f, size.height * 0.5f), Offset(size.width * 0.5f, size.height * 0.75f), strokeWidth = stroke, cap = StrokeCap.Round)
    }

    private fun DrawScope.drawHome(tint: Color) {
        val stroke = size.width * 0.1f
        val path = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.18f)
            lineTo(size.width * 0.82f, size.height * 0.45f)
            lineTo(size.width * 0.82f, size.height * 0.82f)
            lineTo(size.width * 0.18f, size.height * 0.82f)
            lineTo(size.width * 0.18f, size.height * 0.45f)
            close()
        }
        drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }

    private fun DrawScope.drawPerson(tint: Color) {
        val stroke = size.width * 0.1f
        drawCircle(color = tint, radius = size.width * 0.18f, center = Offset(size.width * 0.5f, size.height * 0.32f), style = Stroke(width = stroke))
        val bodyPath = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.82f)
            cubicTo(size.width * 0.2f, size.height * 0.58f, size.width * 0.8f, size.height * 0.58f, size.width * 0.8f, size.height * 0.82f)
        }
        drawPath(bodyPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }

    private fun DrawScope.drawSettings(tint: Color) {
        val stroke = size.width * 0.1f
        drawCircle(color = tint, radius = size.width * 0.18f, center = Offset(size.width * 0.5f, size.height * 0.5f), style = Stroke(width = stroke))
        drawCircle(color = tint, radius = size.width * 0.34f, center = Offset(size.width * 0.5f, size.height * 0.5f), style = Stroke(width = stroke * 0.8f))
    }

    private fun DrawScope.drawInfo(tint: Color) {
        val stroke = size.width * 0.1f
        drawCircle(color = tint, radius = size.width * 0.38f, center = Offset(size.width * 0.5f, size.height * 0.5f), style = Stroke(width = stroke))
        drawCircle(color = tint, radius = stroke * 0.6f, center = Offset(size.width * 0.5f, size.height * 0.33f))
        drawLine(tint, Offset(size.width * 0.5f, size.height * 0.46f), Offset(size.width * 0.5f, size.height * 0.7f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}
