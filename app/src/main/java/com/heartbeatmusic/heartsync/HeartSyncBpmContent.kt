package com.heartbeatmusic.heartsync

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private fun ActivityMode.primaryColor(): Color = when (this) {
    ActivityMode.CALM -> Color(0xFF4A90A4)
    ActivityMode.DRIVING -> Color(0xFFE07C3C)
    ActivityMode.EXERCISE -> Color(0xFFD84315)
}

private fun ActivityMode.scaleRange(): Pair<Float, Float> = when (this) {
    ActivityMode.CALM -> 0.85f to 1.2f
    ActivityMode.DRIVING -> 0.82f to 1.22f
    ActivityMode.EXERCISE -> 0.88f to 1.15f
}

/**
 * Compose UI for displaying real-time heart rate BPM.
 * Uses collectAsStateWithLifecycle to observe ViewModel's currentHeartRate and update the display.
 *
 * Integration (in MainActivity):
 * ```java
 * ComposeView composeView = findViewById(R.id.compose_bpm);
 * composeView.setContent(compose -> {
 *     HeartSyncBpmContentKt.HeartSyncBpmContent(compose);
 * });
 * ```
 *
 * Or with Kotlin Activity:
 * ```kotlin
 * setContent {
 *     HeartSyncBpmContent()
 * }
 * ```
 */
fun setHeartSyncBpmContent(composeView: androidx.compose.ui.platform.ComposeView) {
    composeView.setContent {
        HeartSyncBpmContent()
    }
}

@Composable
fun HeartSyncBpmContent(
    modifier: Modifier = Modifier,
    viewModel: HeartSyncViewModel = viewModel()
) {
    val heartRate by viewModel.currentHeartRate.collectAsStateWithLifecycle()
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
    val circleColor by animateColorAsState(
        targetValue = currentMode.primaryColor().copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 500),
        label = "circleColor"
    )
    val (scaleMin, scaleMax) = currentMode.scaleRange()
    val scale = remember { Animatable(scaleMin) }
    val updatedHeartRate = rememberUpdatedState(heartRate)

    LaunchedEffect(currentMode) {
        scale.snapTo(scaleMin)
        while (true) {
            val duration = (60000 / updatedHeartRate.value.coerceAtLeast(1)).toInt()
            scale.animateTo(
                targetValue = scaleMax,
                animationSpec = tween(durationMillis = duration)
            )
            scale.animateTo(
                targetValue = scaleMin,
                animationSpec = tween(durationMillis = duration)
            )
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Song bpm:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(8.dp)
                .size(56.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        clip = false
                    }
            ) {
                drawCircle(
                    color = circleColor,
                    radius = size.minDimension / 2f,
                    center = center
                )
            }
            AnimatedContent(
                targetState = heartRate,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "bpmNumber"
            ) { targetBpm ->
                Text(
                    text = targetBpm.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)
                )
            }
        }
    }
}
