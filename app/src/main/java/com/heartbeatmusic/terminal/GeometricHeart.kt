package com.heartbeatmusic.terminal

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heartbeatmusic.heartsync.HeartSyncViewModel
import androidx.compose.material3.Text

private val CyanGlow = Color(0xFF00FFFF)
private val ZenPurple = Color(0xFFE0B0FF)
private val OverdriveCyan = Color(0xFF40FFFF)

private fun bpmToDurationMs(bpm: Int): Int = when {
    bpm <= 0 -> 1000
    else -> (60000 / bpm).coerceIn(300, 2000)
}

private fun TerminalMode.strokeColor(): Color = when (this) {
    TerminalMode.ZEN -> ZenPurple
    TerminalMode.SYNC -> CyanGlow
    TerminalMode.OVERDRIVE -> OverdriveCyan
}

private fun TerminalMode.breathMultiplier(): Float = when (this) {
    TerminalMode.ZEN -> 1.8f
    TerminalMode.SYNC -> 1.0f
    TerminalMode.OVERDRIVE -> 0.6f
}

/** Geometric heart path (Compose Path) */
private fun createHeartPath(size: Float): Path {
    val s = minOf(size, size) * 0.4f
    val cx = size / 2f
    val cy = size / 2f

    return Path().apply {
        moveTo(cx, cy - 0.35f * s)
        cubicTo(cx + 0.5f * s, cy - 0.35f * s, cx + 0.5f * s, cy + 0.1f * s, cx, cy + 0.4f * s)
        cubicTo(cx - 0.5f * s, cy + 0.1f * s, cx - 0.5f * s, cy - 0.35f * s, cx, cy - 0.35f * s)
        close()
    }
}

/** Android Path for BlurMaskFilter (same shape) */
private fun createAndroidHeartPath(size: Float): android.graphics.Path {
    val s = minOf(size, size) * 0.4f
    val cx = size / 2f
    val cy = size / 2f

    return android.graphics.Path().apply {
        moveTo(cx, cy - 0.35f * s)
        cubicTo(cx + 0.5f * s, cy - 0.35f * s, cx + 0.5f * s, cy + 0.1f * s, cx, cy + 0.4f * s)
        cubicTo(cx - 0.5f * s, cy + 0.1f * s, cx - 0.5f * s, cy - 0.35f * s, cx, cy - 0.35f * s)
        close()
    }
}

/** Vertex points for dot drawing (approximate from heart path) */
private fun heartVertices(size: Float): List<Offset> {
    val w = size
    val h = size
    val cx = w / 2f
    val cy = h / 2f
    val s = minOf(w, h) * 0.4f
    return listOf(
        Offset(cx, cy - 0.35f * s),
        Offset(cx + 0.25f * s, cy - 0.1f * s),
        Offset(cx + 0.5f * s, cy + 0.05f * s),
        Offset(cx, cy + 0.4f * s),
        Offset(cx - 0.5f * s, cy + 0.05f * s),
        Offset(cx - 0.25f * s, cy - 0.1f * s),
    )
}

@Composable
fun GeometricHeartContent(
    modifier: Modifier = Modifier,
    viewModel: HeartSyncViewModel
) {
    val currentBpm by viewModel.currentHeartRate.collectAsStateWithLifecycle()
    val mode by TerminalModeHolder.selectedMode.collectAsStateWithLifecycle()
    val strokeColor = mode.strokeColor()
    val breathMult = mode.breathMultiplier()

    val heartScale = remember { Animatable(1f) }
    val updatedBpm = rememberUpdatedState(currentBpm.coerceAtLeast(1))

    LaunchedEffect(mode) {
        heartScale.snapTo(1f)
    }

    LaunchedEffect(mode, currentBpm) {
        while (true) {
            val cycleMs = (bpmToDurationMs(updatedBpm.value) * breathMult).toInt().coerceAtLeast(400)
            heartScale.animateTo(
                targetValue = 1.12f,
                animationSpec = keyframes {
                    durationMillis = cycleMs
                    1f at 0
                    1.06f at (cycleMs * 0.25f).toInt()
                    1.12f at (cycleMs * 0.45f).toInt()
                    1.08f at (cycleMs * 0.6f).toInt()
                    1f at cycleMs
                }
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "jitter")
    val jitterX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jitterX"
    )

    val vertexAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vertexAlpha"
    )

    val jitter = if (mode == TerminalMode.OVERDRIVE) (jitterX - 0.5f) * 0.02f else 0f
    val scaleWithJitter = heartScale.value + jitter

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        scaleX = scaleWithJitter
                        scaleY = scaleWithJitter
                    }
            ) {
                val path = createHeartPath(size.minDimension)
                val vertices = heartVertices(size.minDimension)

                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        color = strokeColor.toArgb()
                        style = Paint.Style.STROKE
                        strokeWidth = 3f
                        isAntiAlias = true
                        setMaskFilter(BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL))
                    }
                    drawPath(createAndroidHeartPath(size.minDimension), paint)
                }

                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                vertices.forEach { vertex ->
                    drawCircle(
                        color = strokeColor.copy(alpha = vertexAlpha),
                        radius = 4f,
                        center = vertex
                    )
                }
            }

            Text(
                text = currentBpm.toString(),
                fontFamily = FontFamily.Monospace,
                fontSize = 28.sp,
                color = strokeColor,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

