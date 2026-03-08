package com.heartbeatmusic.heartsync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
        Text(
            text = heartRate.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)
        )
    }
}
