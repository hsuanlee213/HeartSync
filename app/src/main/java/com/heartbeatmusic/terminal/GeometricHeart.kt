package com.heartbeatmusic.terminal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.heartbeatmusic.R
import com.heartbeatmusic.heartsync.HeartSyncViewModel
import kotlin.math.sin

private val CyanGlow = Color(0xFF00FFFF)
private val ZenPurple = Color(0xFFE0B0FF)
private val OverdriveCyan = Color(0xFF40FFFF)
private val PanelAccent = Color(0xFF6366F1)
private val StopRed = Color(0xFFEF4444)
private val PanelBg = Color(0xFF1A1A2E)
private val SliderInactive = Color(0xFF333333)
private const val TRANSITION_MS = 500
private val COLLAPSED_PANEL_HEIGHT = 72.dp

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
    val isMusicPlaying by viewModel.isMusicPlaying.collectAsStateWithLifecycle()
    val isPanelExpanded by viewModel.isPanelExpanded.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.syncPlaybackState() }
    val currentTrackTitle by viewModel.currentTrackTitle.collectAsStateWithLifecycle()
    val currentTrackArtist by viewModel.currentTrackArtist.collectAsStateWithLifecycle()
    val currentCoverUrl by viewModel.currentCoverUrl.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val mode by TerminalModeHolder.selectedMode.collectAsStateWithLifecycle()
    val strokeColor = mode.strokeColor()
    val breathMult = mode.breathMultiplier()

    val heartOffsetY by animateDpAsState(
        targetValue = if (isPanelExpanded) (-48).dp else 0.dp,
        animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing),
        label = "heartOffset"
    )
    val heartBaseScale by animateFloatAsState(
        targetValue = if (isPanelExpanded) 0.9f else 1f,
        animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing),
        label = "heartScale"
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

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val expandedHeightDp: Dp = maxHeight * 0.4f

        val panelHeight by animateDpAsState(
            targetValue = if (isPanelExpanded) expandedHeightDp else COLLAPSED_PANEL_HEIGHT,
            animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing),
            label = "panelHeight"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Heart + BPM in same Column (move together); no clip on container, 48dp top padding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = heartOffsetY)
                ) {
                    // Heart + visualizers
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        SideVisualizer(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 8.dp)
                                .graphicsLayer { alpha = visualizerAlpha },
                            isActive = isMusicPlaying
                        )

                        Image(
                            painter = painterResource(R.drawable.login_logo_remove_background),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .graphicsLayer {
                                    scaleX = effectiveScale
                                    scaleY = effectiveScale
                                    translationX = tremorX
                                    translationY = tremorY
                                }
                        )

                        SideVisualizer(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                                .graphicsLayer { alpha = visualizerAlpha },
                            isActive = isMusicPlaying
                        )
                    }

                    // BPM: 16dp below heart, moves with heart
                    androidx.compose.material3.Text(
                        text = currentBpm.toString(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 32.sp,
                        color = strokeColor,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // MusicPlayerPanel: frosted glass style, 28dp top corners, cyan gradient border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(panelHeight)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(PanelBg.copy(alpha = 0.85f))
                    .then(
                        Modifier.drawWithContent {
                            drawContent()
                            // Top edge gradient: transparent -> cyan -> transparent
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        CyanGlow,
                                        Color.Transparent
                                    )
                                ),
                                topLeft = Offset.Zero,
                                size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx())
                            )
                        }
                    )
            ) {
                AnimatedContent(
                    targetState = isPanelExpanded,
                    transitionSpec = {
                        (fadeIn(tween(TRANSITION_MS)) + slideInVertically(
                            animationSpec = tween(TRANSITION_MS),
                            initialOffsetY = { it }
                        )).togetherWith(
                            fadeOut(tween(TRANSITION_MS)) + slideOutVertically(
                                animationSpec = tween(TRANSITION_MS),
                                targetOffsetY = { -it / 4 }
                            )
                        )
                    },
                    label = "panelContent"
                ) { expanded ->
                    if (expanded) {
                        ExpandedPanel(
                            trackTitle = currentTrackTitle,
                            artistName = currentTrackArtist,
                            coverUrl = currentCoverUrl,
                            progress = playbackProgress,
                            onProgressChange = { viewModel.seekToProgress(it) },
                            onPrevious = { viewModel.previous() },
                            onPlayPause = { viewModel.playPause() },
                            onNext = { viewModel.next() },
                            onStop = { viewModel.stop() },
                            isPlaying = isMusicPlaying
                        )
                    } else {
                        CollapsedPanel(onPlayClick = { viewModel.playPause() })
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsedPanel(onPlayClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onPlayClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = CyanGlow,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun ExpandedPanel(
    trackTitle: String,
    artistName: String,
    coverUrl: String?,
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    isPlaying: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress Slider at top: cyan active, dark gray inactive, smaller thumb
        Slider(
            value = progress,
            onValueChange = onProgressChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = CyanGlow,
                activeTrackColor = CyanGlow,
                inactiveTrackColor = SliderInactive
            )
        )

        // Middle: Row with album art (cyan border), song name (Bold), artist
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // AlbumArt: circular with 1.dp cyan border
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, CyanGlow, CircleShape)
                    .drawWithContent {
                        drawContent()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (coverUrl != null && coverUrl.isNotEmpty()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_music_note),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(CyanGlow)
                    )
                }
            }

            // Song title (White) + artist (LightGray)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                androidx.compose.material3.Text(
                    text = trackTitle.ifEmpty { "Not playing" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                androidx.compose.material3.Text(
                    text = artistName.ifEmpty { "" },
                    fontSize = 12.sp,
                    color = Color.LightGray.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bottom: Stop (red glow border) | Previous | Play/Pause | Next (all cyan)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, StopRed.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop",
                        tint = CyanGlow,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = CyanGlow,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = CyanGlow,
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = CyanGlow,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
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
