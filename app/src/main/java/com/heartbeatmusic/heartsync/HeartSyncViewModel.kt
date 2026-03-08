package com.heartbeatmusic.heartsync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * HeartSync ViewModel.
 * Exposes heart rate as StateFlow for UI observation.
 * Uses MockHeartRateProvider with viewModelScope (no-arg constructor for ViewModelProvider).
 */
class HeartSyncViewModel : ViewModel() {

    private val heartRateProvider = MockHeartRateProvider(viewModelScope, ActivityMode.CALM)

    private val _currentHeartRate = MutableStateFlow(0)
    val currentHeartRate: StateFlow<Int> = _currentHeartRate.asStateFlow()

    private val _currentMode = MutableStateFlow(ActivityMode.CALM)
    val currentMode: StateFlow<ActivityMode> = _currentMode.asStateFlow()

    init {
        collectHeartRate()
        startMonitoring()
    }

    private fun collectHeartRate() {
        heartRateProvider.heartRateFlow
            .onEach { bpm ->
                _currentHeartRate.value = bpm
            }
            .launchIn(viewModelScope)
    }

    /**
     * Start monitoring heart rate.
     */
    fun startMonitoring() {
        viewModelScope.launch {
            heartRateProvider.startMonitoring()
        }
    }

    /**
     * Stop monitoring heart rate.
     */
    fun stopMonitoring() {
        heartRateProvider.stopMonitoring()
    }

    /**
     * Switch activity mode, affects BPM range emitted by Mock provider.
     */
    fun setMode(mode: ActivityMode) {
        _currentMode.value = mode
        if (heartRateProvider is MockHeartRateProvider) {
            heartRateProvider.setMode(mode)
        }
    }

    override fun onCleared() {
        super.onCleared()
        heartRateProvider.stopMonitoring()
    }
}
