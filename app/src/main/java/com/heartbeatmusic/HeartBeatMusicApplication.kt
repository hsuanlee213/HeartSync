package com.heartbeatmusic

import android.app.Application
import com.heartbeatmusic.player.MusicDiscoveryPlayer

class HeartBeatMusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MusicDiscoveryPlayer.init(this)
    }
}
