package com.heartbeatmusic.player

import android.content.Context
import android.media.AudioAttributes as AndroidAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.heartbeatmusic.data.model.Song
import com.heartbeatmusic.data.remote.JamendoApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Singleton Music Discovery Player using Media3 ExoPlayer.
 * - Jamendo API integration (mock URLs for now)
 * - playNextByBpm(bpm) for BPM-based song discovery
 * - Audio Focus handling
 * - Volume fade-in/fade-out
 * - CoroutineScope for async loading
 */
object MusicDiscoveryPlayer {

    private var player: ExoPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadJob: Job? = null

    private const val FADE_DURATION_MS = 400L
    private const val FADE_STEPS = 20
    private const val BPM_TOLERANCE = 15

    /**
     * Initialize the player. Must be called with Application context before use.
     */
    fun init(context: Context) {
        val appContext = context.applicationContext
        if (player != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(appContext)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ false)
            .build()
            .also { p ->
                p.repeatMode = Player.REPEAT_MODE_OFF
            }

        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun getPlayer(): ExoPlayer? = player

    fun getCurrentSong(): Song? = _currentSong
    private var _currentSong: Song? = null

    /**
     * Play next song matching the given BPM.
     * Uses CoroutineScope to load asynchronously without blocking UI.
     * Ensure [init] has been called (e.g. from Application.onCreate).
     */
    fun playNextByBpm(bpm: Int) {
        if (player == null) return
        loadJob?.cancel()
        loadJob = scope.launch {
            val result = JamendoApiService.findTracksByBpm(bpm, BPM_TOLERANCE)
            result.fold(
                onSuccess = { songs ->
                    if (songs.isNotEmpty()) {
                        val song = songs.random()
                        withContext(Dispatchers.Main) {
                            playSong(song)
                        }
                    }
                },
                onFailure = { /* handle error */ }
            )
        }
    }

    /**
     * Play a specific song with fade-in.
     */
    fun playSong(song: Song) {
        val url = song.audioUrl ?: return
        val p = player ?: return

        requestAudioFocus()
        _currentSong = song

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(url))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setArtworkUri(song.coverUrl?.let { Uri.parse(it) })
                    .build()
            )
            .build()

        p.setMediaItem(mediaItem)
        p.prepare()
        p.volume = 0f
        p.play()

        scope.launch {
            fadeIn()
        }
    }

    fun pause() {
        scope.launch {
            fadeOut {
                player?.pause()
                abandonAudioFocus()
            }
        }
    }

    fun stop() {
        loadJob?.cancel()
        scope.launch {
            fadeOut {
                player?.stop()
                player?.clearMediaItems()
                _currentSong = null
                abandonAudioFocus()
            }
        }
    }

    fun release() {
        loadJob?.cancel()
        scope.cancel()
        abandonAudioFocus()
        player?.release()
        player = null
        audioManager = null
        audioFocusRequest = null
    }

    // --- Audio Focus ---

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false
        if (hasAudioFocus) return true

        val attrs = AndroidAudioAttributes.Builder()
            .setUsage(AndroidAudioAttributes.USAGE_MEDIA)
            .setContentType(AndroidAudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange -> onAudioFocusChange(focusChange) }
                .build()
                .also { audioFocusRequest = it }
        } else {
            null
        }

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            am.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                { focusChange -> onAudioFocusChange(focusChange) },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (!hasAudioFocus) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
        }
        hasAudioFocus = false
    }

    private fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                scope.launch {
                    fadeOut {
                        player?.pause()
                        hasAudioFocus = false
                    }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                scope.launch {
                    duckVolume(0.3f)
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                scope.launch {
                    hasAudioFocus = true
                    player?.play()
                    fadeIn(durationMs = 200)
                }
            }
        }
    }

    // --- Volume Fade ---

    private suspend fun duckVolume(targetVolume: Float) {
        val p = player ?: return
        val startVolume = p.volume
        val stepMs = 100L / FADE_STEPS
        val stepDelta = (startVolume - targetVolume) / FADE_STEPS
        for (i in 1..FADE_STEPS) {
            p.volume = (startVolume - i * stepDelta).coerceIn(0f, 1f)
            delay(stepMs)
        }
        p.volume = targetVolume
    }

    private suspend fun fadeIn(durationMs: Long = FADE_DURATION_MS) {
        val p = player ?: return
        val stepMs = durationMs / FADE_STEPS
        val stepVolume = 1f / FADE_STEPS
        for (i in 1..FADE_STEPS) {
            p.volume = (i * stepVolume).coerceIn(0f, 1f)
            delay(stepMs)
        }
        p.volume = 1f
    }

    private suspend fun fadeOut(
        durationMs: Long = FADE_DURATION_MS,
        onComplete: suspend () -> Unit
    ) {
        val p = player ?: return
        val stepMs = durationMs / FADE_STEPS
        val stepVolume = 1f / FADE_STEPS
        val startVolume = p.volume
        for (i in FADE_STEPS downTo 0) {
            p.volume = (i * stepVolume * startVolume).coerceIn(0f, 1f)
            delay(stepMs)
        }
        p.volume = 0f
        onComplete()
    }
}
