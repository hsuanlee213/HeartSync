package com.heartbeatmusic.terminal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heartbeatmusic.R
import com.heartbeatmusic.heartsync.HeartSyncViewModel
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

    // Heartbeat scale animation: Lub-Dub rhythm synced to BPM
    LaunchedEffect(mode) {
        heartScale.snapTo(1f)
    }

    LaunchedEffect(mode, currentBpm) {
        while (true) {
            val cycleMs = (bpmToDurationMs(updatedBpm.value) * breathMult).toInt().coerceAtLeast(500)
            heartScale.animateTo(
                targetValue = 1.12f,
                animationSpec = keyframes {
                    durationMillis = cycleMs
                    1f at 0
                    1.1f at (cycleMs * 0.08f).toInt()   // S1 peak
                    1.02f at (cycleMs * 0.2f).toInt()   // S1 fall
                    1.1f at (cycleMs * 0.3f).toInt()    // S2 peak
                    1.02f at (cycleMs * 0.4f).toInt()   // S2 fall
                    1f at cycleMs                       // rest
                }
            )
        }
    }

    // OVERDRIVE tremor: subtle jitter
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

    val scaleWithJitter = heartScale.value + (if (mode == TerminalMode.OVERDRIVE) (jitterX - 0.5f) * 0.015f else 0f)
    val tremorX = if (mode == TerminalMode.OVERDRIVE) sin(jitterX * 6.28f) * 2f else 0f
    val tremorY = if (mode == TerminalMode.OVERDRIVE) sin(jitterY * 6.28f + 1.5f) * 2f else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Heart image: 50-55% screen width, compact at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.login_logo_remove_background),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .graphicsLayer {
                        scaleX = scaleWithJitter
                        scaleY = scaleWithJitter
                        translationX = tremorX
                        translationY = tremorY
                    }
            )
        }

        // BPM: 24dp below heart
        androidx.compose.material3.Text(
            text = currentBpm.toString(),
            fontFamily = FontFamily.Monospace,
            fontSize = 32.sp,
            color = strokeColor,
            modifier = Modifier.padding(top = 24.dp)
        )

        // Minimal gap to control area (playback bar + mode buttons) for compact HUD
        Spacer(modifier = Modifier.height(8.dp))
    }
}
