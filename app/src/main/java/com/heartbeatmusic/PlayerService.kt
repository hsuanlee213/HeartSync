package com.heartbeatmusic

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Background playback service that wraps the shared ExoPlayer instance in a MediaSession.
 * This enables:
 *  - Lock screen media controls (play/pause/seek)
 *  - Notification with playback controls
 *  - Headphone/Bluetooth media button support
 *  - Audio focus management (auto-pause on calls, duck on navigation voice)
 *  - System process keep-alive while music is playing
 */
class PlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = PlayerHolder.getInstance(this).player
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
