package com.heartbeatmusic.heartsync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.heartbeatmusic.PlayerHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    private val _currentTrackArtist = MutableStateFlow("")
    val currentTrackArtist: StateFlow<String> = _currentTrackArtist.asStateFlow()

    private val _currentCoverUrl = MutableStateFlow<String?>(null)
    val currentCoverUrl: StateFlow<String?> = _currentCoverUrl.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val player = PlayerHolder.getInstance(application).getPlayer()
    private var progressJob: Job? = null

    init {
        collectHeartRate()
        startMonitoring()
        observePlaybackState()
    }

    private fun observePlaybackState() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    _isMusicPlaying.value = isPlaying
                    if (isPlaying) startProgressUpdates() else stopProgressUpdates()
                }
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    val md = mediaItem?.mediaMetadata
                    _currentTrackTitle.value = md?.title?.toString() ?: ""
                    _currentTrackArtist.value = md?.artist?.toString() ?: ""
                    _currentCoverUrl.value = md?.artworkUri?.toString()
                }
            }
        })
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _isMusicPlaying.value = player.isPlaying
            val md = player.currentMediaItem?.mediaMetadata
            _currentTrackTitle.value = md?.title?.toString() ?: ""
            _currentTrackArtist.value = md?.artist?.toString() ?: ""
            _currentCoverUrl.value = md?.artworkUri?.toString()
            if (player.isPlaying) startProgressUpdates()
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val dur = player.duration
                val pos = player.currentPosition
                _playbackProgress.value = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
                delay(300)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    /** Sync playback state from player (e.g. when UI becomes visible). */
    fun syncPlaybackState() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _isMusicPlaying.value = player.isPlaying
            val md = player.currentMediaItem?.mediaMetadata
            _currentTrackTitle.value = md?.title?.toString() ?: ""
            _currentTrackArtist.value = md?.artist?.toString() ?: ""
            _currentCoverUrl.value = md?.artworkUri?.toString()
            if (player.isPlaying) startProgressUpdates()
        }
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
