package com.heartbeatmusic.heartsync

import com.heartbeatmusic.terminal.TerminalMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Mock heart rate data provider.
 * Simulates different BPM ranges based on current mode (ZEN/SYNC/OVERDRIVE), emits random value every second.
 *
 * @param scope CoroutineScope for launching monitoring (use viewModelScope to avoid memory leaks)
 * @param initialMode Initial terminal mode
 */
class MockHeartRateProvider(
    private val scope: CoroutineScope,
    private var currentMode: TerminalMode = TerminalMode.SYNC
) : HeartRateProvider {

    private val _heartRateFlow = MutableSharedFlow<Int>(replay = 1)
    override val heartRateFlow: SharedFlow<Int> = _heartRateFlow

    private var monitoringJob: Job? = null

    /**
     * Update current mode, affects subsequent BPM range emissions.
     */
    fun setMode(mode: TerminalMode) {
        currentMode = mode
    }

    override fun startMonitoring() {
        stopMonitoring()
        monitoringJob = scope.launch {
            while (scope.isActive) {
                val bpm = Random.nextInt(currentMode.minBpm, currentMode.maxBpm + 1)
                _heartRateFlow.emit(bpm)
                delay(1000L)
            }
        }
    }

    override fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
}
