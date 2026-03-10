package com.heartbeatmusic.heartsync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.heartbeatmusic.PlayerHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import androidx.media3.common.Player

/**
 * HeartSync ViewModel.
 * Exposes heart rate and playback state for UI observation.
 */
class HeartSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val heartRateProvider = MockHeartRateProvider(viewModelScope, ActivityMode.CALM)

    private val _currentHeartRate = MutableStateFlow(0)
    val currentHeartRate: StateFlow<Int> = _currentHeartRate.asStateFlow()

    private val _currentMode = MutableStateFlow(ActivityMode.CALM)
    val currentMode: StateFlow<ActivityMode> = _currentMode.asStateFlow()

    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying: StateFlow<Boolean> = _isMusicPlaying.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow("")
    val currentTrackTitle: StateFlow<String> = _currentTrackTitle.asStateFlow()

    private val player = PlayerHolder.getInstance(application).getPlayer()

    init {
        collectHeartRate()
        startMonitoring()
        observePlaybackState()
    }

    private fun observePlaybackState() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isMusicPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val title = mediaItem?.mediaMetadata?.title?.toString() ?: ""
                _currentTrackTitle.value = title
            }
        })
        _isMusicPlaying.value = player.isPlaying
        _currentTrackTitle.value = player.currentMediaItem?.mediaMetadata?.title?.toString() ?: ""
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
