package com.heartbeatmusic

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

/**
 * Application-scoped holder for a single ExoPlayer instance so different
 * Activities can share playback state and UI.
 */
class PlayerHolder private constructor(appContext: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(appContext).build()

    fun release() {
        player.release()
    }

    companion object {
        @Volatile
        private var instance: PlayerHolder? = null

        @Synchronized
        fun getInstance(context: Context): PlayerHolder {
            return instance ?: PlayerHolder(context.applicationContext).also { instance = it }
        }
    }
}
