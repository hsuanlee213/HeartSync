package com.heartbeatmusic.data.remote

import com.heartbeatmusic.BuildConfig
import com.heartbeatmusic.data.model.Song
import com.heartbeatmusic.terminal.TerminalMode
import javax.inject.Inject

class JamendoRepository @Inject constructor(private val api: JamendoApiService) {
    suspend fun fetchTracksForMode(mode: TerminalMode): List<Song> {
        val params = mode.toJamendoParams()
        val response = api.getTracks(
            clientId = BuildConfig.JAMENDO_CLIENT_ID,
            speed = params.speed,
            fuzzytags = params.fuzzytags,
            vocalInstrumental = params.vocalInstrumental
        )
        if (mode == TerminalMode.OVERDRIVE && response.results.size < 3) {
            return api.getTracks(
                clientId = BuildConfig.JAMENDO_CLIENT_ID,
                speed = "veryhigh",
                fuzzytags = "electronic+rock+party+powerful+action"
            ).results.map { it.toSong() }
        }
        return response.results.map { it.toSong() }
    }
}

private fun JamendoTrack.toSong() = Song().apply {
    id = this@toSong.id
    title = name
    artist = artistName
    audioUrl = audio
    coverUrl = image
}

data class JamendoParams(val speed: String, val fuzzytags: String, val vocalInstrumental: String? = null)

fun TerminalMode.toJamendoParams() = when (this) {
    TerminalMode.ZEN -> JamendoParams(
        speed = "verylow",
        fuzzytags = "relaxation+ambient+meditation+piano+instrumental",
        vocalInstrumental = "instrumental"
    )
    TerminalMode.SYNC -> JamendoParams(
        speed = "medium",
        fuzzytags = "pop+acoustic+indie+lounge+groove"
    )
    TerminalMode.OVERDRIVE -> JamendoParams(
        speed = "high",
        fuzzytags = "electronic+rock+dance+energetic+workout"
    )
}
