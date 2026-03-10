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
import kotlin.math.sin

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

/**
 * Heart outline using Bezier curves.
 * Two rounded arcs at top, pointed bottom - low-poly 3D style.
 */
private fun createHeartOutlinePath(size: Float): Path {
    val s = minOf(size, size) * 0.42f
    val cx = size / 2f
    val cy = size / 2f

    return Path().apply {
        moveTo(cx, cy - 0.38f * s)
        // Right lobe: top arc down to bottom
        cubicTo(
            cx + 0.48f * s, cy - 0.38f * s,
            cx + 0.52f * s, cy + 0.02f * s,
            cx + 0.38f * s, cy + 0.28f * s
        )
        cubicTo(
            cx + 0.22f * s, cy + 0.48f * s,
            cx, cy + 0.52f * s,
            cx, cy + 0.52f * s
        )
        // Left lobe: bottom back to top
        cubicTo(
            cx, cy + 0.52f * s,
            cx - 0.22f * s, cy + 0.48f * s,
            cx - 0.38f * s, cy + 0.28f * s
        )
        cubicTo(
            cx - 0.52f * s, cy + 0.02f * s,
            cx - 0.48f * s, cy - 0.38f * s,
            cx, cy - 0.38f * s
        )
        close()
    }
}

private fun createAndroidHeartPath(size: Float): android.graphics.Path {
    val s = minOf(size, size) * 0.42f
    val cx = size / 2f
    val cy = size / 2f

    return android.graphics.Path().apply {
        moveTo(cx, cy - 0.38f * s)
        cubicTo(cx + 0.48f * s, cy - 0.38f * s, cx + 0.52f * s, cy + 0.02f * s, cx + 0.38f * s, cy + 0.28f * s)
        cubicTo(cx + 0.22f * s, cy + 0.48f * s, cx, cy + 0.52f * s, cx, cy + 0.52f * s)
        cubicTo(cx, cy + 0.52f * s, cx - 0.22f * s, cy + 0.48f * s, cx - 0.38f * s, cy + 0.28f * s)
        cubicTo(cx - 0.52f * s, cy + 0.02f * s, cx - 0.48f * s, cy - 0.38f * s, cx, cy - 0.38f * s)
        close()
    }
}

/** Internal wireframe lines (3-4 diagonal cuts for low-poly feel) */
private fun createInternalWireframe(size: Float): List<Pair<Offset, Offset>> {
    val s = minOf(size, size) * 0.42f
    val cx = size / 2f
    val cy = size / 2f

    return listOf(
        // Top-left to bottom
        Offset(cx - 0.25f * s, cy - 0.2f * s) to Offset(cx, cy + 0.4f * s),
        // Top-right to bottom
        Offset(cx + 0.25f * s, cy - 0.2f * s) to Offset(cx, cy + 0.4f * s),
        // Left lobe to right lobe (horizontal cut)
        Offset(cx - 0.3f * s, cy + 0.1f * s) to Offset(cx + 0.3f * s, cy + 0.1f * s),
        // Center vertical
        Offset(cx, cy - 0.35f * s) to Offset(cx, cy + 0.45f * s),
    )
}

/** Heart center (core point) */
private fun heartCenter(size: Float): Offset {
    val cx = size / 2f
    val cy = size / 2f
    val s = minOf(size, size) * 0.42f
    return Offset(cx, cy + 0.08f * s)
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

    // S1/S2 lub-dub rhythm: "砰-砰---" two quick beats then pause
    LaunchedEffect(mode) {
        heartScale.snapTo(1f)
    }

    LaunchedEffect(mode, currentBpm) {
        while (true) {
            val cycleMs = (bpmToDurationMs(updatedBpm.value) * breathMult).toInt().coerceAtLeast(500)
            // S1 at 0-12%, S2 at 25-37%, diastole 37-100%
            heartScale.animateTo(
                targetValue = 1.12f,
                animationSpec = keyframes {
                    durationMillis = cycleMs
                    1f at 0
                    1.1f at (cycleMs * 0.08f).toInt()   // S1 peak
                    1.02f at (cycleMs * 0.2f).toInt()   // S1 fall
                    1.1f at (cycleMs * 0.3f).toInt()    // S2 peak
                    1.02f at (cycleMs * 0.4f).toInt()  // S2 fall
                    1f at cycleMs                       // rest
                }
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "jitter")
    val jitterX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(60, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jitterX"
    )
    val jitterY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jitterY"
    )

    val coreAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreAlpha"
    )

    val scaleWithJitter = heartScale.value + (if (mode == TerminalMode.OVERDRIVE) (jitterX - 0.5f) * 0.015f else 0f)

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
                    .size(200.dp)
                    .graphicsLayer {
                        scaleX = scaleWithJitter
                        scaleY = scaleWithJitter
                    }
            ) {
                val path = createHeartOutlinePath(size.minDimension)
                val wireframe = createInternalWireframe(size.minDimension)
                val center = heartCenter(size.minDimension)

                val n = drawContext.canvas.nativeCanvas

                // 1. Outline glow (strong)
                n.apply {
                    val glowPaint = Paint().apply {
                        color = CyanGlow.toArgb()
                        style = Paint.Style.STROKE
                        strokeWidth = 4f
                        isAntiAlias = true
                        setMaskFilter(BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL))
                    }
                    drawPath(createAndroidHeartPath(size.minDimension), glowPaint)
                }

                // 2. Outline stroke (sharp)
                drawPath(
                    path = path,
                    color = CyanGlow,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // 3. Internal wireframe (thinner, lower opacity) - with OVERDRIVE jitter
                wireframe.forEachIndexed { i, (start, end) ->
                    val j1 = if (mode == TerminalMode.OVERDRIVE) {
                        Offset(
                            (sin(jitterX * 6.28f + i) * 2.5f),
                            (sin(jitterY * 6.28f + i * 0.7f) * 2.5f)
                        )
                    } else Offset.Zero
                    val j2 = if (mode == TerminalMode.OVERDRIVE) {
                        Offset(
                            (sin(jitterY * 6.28f + i * 1.2f) * 2f),
                            (sin(jitterX * 6.28f + i * 0.5f) * 2f)
                        )
                    } else Offset.Zero
                    drawLine(
                        color = CyanGlow.copy(alpha = 0.5f),
                        start = start + j1,
                        end = end + j2,
                        strokeWidth = 1f,
                        cap = StrokeCap.Round
                    )
                }

                // 4. Heart core - solid dot with strongest glow
                n.apply {
                    val coreGlowPaint = Paint().apply {
                        color = CyanGlow.toArgb()
                        style = Paint.Style.FILL
                        isAntiAlias = true
                        setMaskFilter(BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL))
                    }
                    drawCircle(center.x, center.y, 12f, coreGlowPaint)
                }
                drawCircle(
                    color = CyanGlow.copy(alpha = coreAlpha),
                    radius = 5f,
                    center = center
                )
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
