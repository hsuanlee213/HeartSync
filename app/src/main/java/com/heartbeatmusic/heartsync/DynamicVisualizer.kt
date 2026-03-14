package com.heartbeatmusic.heartsync

import com.heartbeatmusic.terminal.TerminalMode
import androidx.compose.animation.core.Animatable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind

private val ZenPrimary = Color(0xFF4A90A4)
private val ZenSecondary = Color(0xFF2D5A6B)
private val SyncPrimary = Color(0xFFE07C3C)
private val SyncSecondary = Color(0xFFB85C2A)
private val OverdrivePrimary = Color(0xFFD84315)
private val OverdriveSecondary = Color(0xFFBF360C)

private fun TerminalMode.primaryColor(): Color = when (this) {
    TerminalMode.ZEN -> ZenPrimary
    TerminalMode.SYNC -> SyncPrimary
    TerminalMode.OVERDRIVE -> OverdrivePrimary
}

private fun TerminalMode.secondaryColor(): Color = when (this) {
    TerminalMode.ZEN -> ZenSecondary
    TerminalMode.SYNC -> SyncSecondary
    TerminalMode.OVERDRIVE -> OverdriveSecondary
}

/**
 * Full-screen gradient background that smoothly transitions based on [currentMode].
 * Uses drawBehind for efficient rendering.
 */
@Composable
fun DynamicVisualizerBackground(
    modifier: Modifier = Modifier,
    currentMode: TerminalMode
) {
    val primary by animateColorAsState(
        targetValue = currentMode.primaryColor(),
        animationSpec = tween(durationMillis = 800),
        label = "primary"
    )
    val secondary by animateColorAsState(
        targetValue = currentMode.secondaryColor(),
        animationSpec = tween(durationMillis = 800),
        label = "secondary"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(secondary, primary, secondary),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )
            }
    )
}

/**
 * Pulsing circle in the center. Scale animation duration = 60000 / currentBpm ms per beat.
 * Uses graphicsLayer for scale to avoid full Recomposition.
 */
@Composable
fun DynamicVisualizerPulse(
    modifier: Modifier = Modifier,
    currentBpm: Int,
    circleColor: Color = Color.White.copy(alpha = 0.6f)
) {
    val scale = remember { Animatable(0.9f) }
    val durationMillis = bpmToDurationMs(currentBpm)

    LaunchedEffect(currentBpm) {
        while (true) {
            scale.animateTo(
                targetValue = 1.1f,
                animationSpec = tween(durationMillis = durationMillis)
            )
            scale.animateTo(
                targetValue = 0.9f,
                animationSpec = tween(durationMillis = durationMillis)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = minOf(size.minDimension) / 4f
            drawCircle(
                color = circleColor,
                radius = radius,
                center = center
            )
        }
    }
}

/**
 * Full-screen DynamicVisualizer combining gradient background and heartbeat pulse.
 * Observes [currentBpm] and [currentMode] from ViewModel.
 */
@Composable
fun DynamicVisualizer(
    modifier: Modifier = Modifier,
    currentBpm: Int,
    currentMode: TerminalMode
) {
    Box(modifier = modifier.fillMaxSize()) {
        DynamicVisualizerBackground(currentMode = currentMode)
        DynamicVisualizerPulse(
            currentBpm = currentBpm,
            circleColor = Color.White.copy(alpha = 0.5f)
        )
    }
}

/**
 * DynamicVisualizer that observes HeartSyncViewModel.
 * Use with ComposeView: DynamicVisualizerKt.setDynamicVisualizerContent(composeView)
 */
@Composable
fun DynamicVisualizerWithViewModel(
    modifier: Modifier = Modifier,
    viewModel: HeartSyncViewModel = viewModel()
) {
    val currentBpm by viewModel.currentHeartRate.collectAsStateWithLifecycle()
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()

    DynamicVisualizer(
        modifier = modifier,
        currentBpm = currentBpm.coerceAtLeast(1),
        currentMode = currentMode
    )
}

fun setDynamicVisualizerContent(composeView: androidx.compose.ui.platform.ComposeView) {
    composeView.setContent {
        DynamicVisualizerWithViewModel()
    }
}
