package com.heartbeatmusic.terminal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heartbeatmusic.R
import com.heartbeatmusic.heartsync.HeartSyncViewModel
import kotlin.math.sin

private val CyanGlow = Color(0xFF00FFFF)
private val ZenPurple = Color(0xFFE0B0FF)
private val OverdriveCyan = Color(0xFF40FFFF)
private const val TRANSITION_MS = 500

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GeometricHeartContent(
    modifier: Modifier = Modifier,
    viewModel: HeartSyncViewModel
) {
    val currentBpm by viewModel.currentHeartRate.collectAsStateWithLifecycle()
    val isMusicPlaying by viewModel.isMusicPlaying.collectAsStateWithLifecycle()
    val currentTrackTitle by viewModel.currentTrackTitle.collectAsStateWithLifecycle()
    val mode by TerminalModeHolder.selectedMode.collectAsStateWithLifecycle()
    val strokeColor = mode.strokeColor()
    val breathMult = mode.breathMultiplier()

    // Standby vs Active: heart offset & scale (500ms FastOutSlowInEasing)
    val heartOffsetY by animateDpAsState(
        targetValue = if (isMusicPlaying) (-60).dp else 0.dp,
        animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing),
        label = "heartOffset"
    )
    val heartBaseScale by animateFloatAsState(
        targetValue = if (isMusicPlaying) 0.9f else 1f,
        animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing),
        label = "heartScale"
    )
    val musicInfoAlpha by animateFloatAsState(
        targetValue = if (isMusicPlaying) 1f else 0f,
        animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing),
        label = "musicInfoAlpha"
    )
    val visualizerAlpha by animateFloatAsState(
        targetValue = if (isMusicPlaying) 1f else 0f,
        animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing),
        label = "visualizerAlpha"
    )

    val heartScale = remember { Animatable(1f) }
    val updatedBpm = rememberUpdatedState(currentBpm.coerceAtLeast(1))

    LaunchedEffect(mode, currentBpm) {
        while (true) {
            val cycleMs = (bpmToDurationMs(updatedBpm.value) * breathMult).toInt().coerceAtLeast(500)
            heartScale.animateTo(
                targetValue = 1.12f,
                animationSpec = keyframes {
                    durationMillis = cycleMs
                    1f at 0
                    1.1f at (cycleMs * 0.08f).toInt()
                    1.02f at (cycleMs * 0.2f).toInt()
                    1.1f at (cycleMs * 0.3f).toInt()
                    1.02f at (cycleMs * 0.4f).toInt()
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
            animation = tween(60),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "jitterX"
    )
    val jitterY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(80),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "jitterY"
    )

    val scaleWithJitter = heartScale.value + (if (mode == TerminalMode.OVERDRIVE) (jitterX - 0.5f) * 0.015f else 0f)
    val tremorX = if (mode == TerminalMode.OVERDRIVE) sin(jitterX * 6.28f) * 2f else 0f
    val tremorY = if (mode == TerminalMode.OVERDRIVE) sin(jitterY * 6.28f + 1.5f) * 2f else 0f
    val effectiveScale = heartBaseScale * scaleWithJitter

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Heart + side visualizers
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Left visualizer
            SideVisualizer(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .graphicsLayer { alpha = visualizerAlpha },
                isActive = isMusicPlaying
            )

            // Heart image
            Image(
                painter = painterResource(R.drawable.login_logo_remove_background),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .offset(y = heartOffsetY)
                    .graphicsLayer {
                        scaleX = effectiveScale
                        scaleY = effectiveScale
                        translationX = tremorX
                        translationY = tremorY
                    }
            )

            // Right visualizer
            SideVisualizer(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .graphicsLayer { alpha = visualizerAlpha },
                isActive = isMusicPlaying
            )
        }

        // MusicInfoContainer: album art + marquee (between heart and BPM)
        MusicInfoContainer(
            trackTitle = currentTrackTitle,
            modifier = Modifier
                .padding(top = 12.dp)
                .graphicsLayer { alpha = musicInfoAlpha }
        )

        // BPM
        androidx.compose.material3.Text(
            text = currentBpm.toString(),
            fontFamily = FontFamily.Monospace,
            fontSize = 32.sp,
            color = strokeColor,
            modifier = Modifier.padding(top = 24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicInfoContainer(
    trackTitle: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "album")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "albumRotate"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        // Rotating album cover with cyan glow ring
        Box(
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer { rotationZ = rotation }
                .clip(CircleShape)
                .drawWithContent {
                    drawCircle(color = CyanGlow.copy(alpha = 0.25f), radius = size.minDimension / 2 + 4)
                    drawCircle(color = Color(0xFF2A2A2A), radius = size.minDimension / 2)
                    drawContent()
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(CyanGlow)
            )
        }
        Spacer(modifier = Modifier.padding(horizontal = 12.dp))
        androidx.compose.material3.Text(
            text = trackTitle.ifEmpty { "Not playing" },
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = CyanGlow,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .basicMarquee()
        )
    }
}

@Composable
private fun SideVisualizer(
    modifier: Modifier = Modifier,
    isActive: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "viz")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(100),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "vizPhase"
    )

    Canvas(modifier = modifier.size(24.dp, 80.dp)) {
        if (!isActive) return@Canvas
        val barCount = 5
        val barWidth = size.width / (barCount * 2)
        val centerY = size.height / 2
        for (i in 0 until barCount) {
            val h = (sin(phase * 6.28f + i * 1.2f) * 0.5f + 0.5f) * 30f + 8f
            drawRect(
                color = CyanGlow.copy(alpha = 0.6f),
                topLeft = Offset(i * (barWidth * 2), centerY - h / 2),
                size = androidx.compose.ui.geometry.Size(barWidth.coerceAtLeast(1f), h)
            )
        }
    }
}
