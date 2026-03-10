package com.heartbeatmusic.terminal

import androidx.compose.ui.graphics.Color
import com.heartbeatmusic.heartsync.ActivityMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App mode for Terminal. Each mode has predefined music tags and accent color.
 */
enum class TerminalMode(
    val musicTags: String,
    val accentColor: Color,
    val mockTitle: String,
    val mockArtist: String
) {
    ZEN(
        musicTags = "ambient, meditation, piano, soft, peaceful",
        accentColor = Color(0xFF39FF14), // Fluorescent green (matches heart)
        mockTitle = "Eternal Peace",
        mockArtist = "Calm Master"
    ),
    SYNC(
        musicTags = "deep house, lofi, focus, steady, pop",
        accentColor = Color(0xFF00FFFF), // Cyan
        mockTitle = "Digital Pulse",
        mockArtist = "Sync Theory"
    ),
    OVERDRIVE(
        musicTags = "techno, rock, aggressive, powerful, high-tempo",
        accentColor = Color(0xFFFFD700), // Gold (matches heart)
        mockTitle = "System Overload",
        mockArtist = "Kinetic Power"
    )
}

/**
 * Holds the selected TerminalMode with StateFlow.
 * Shared across MainActivity and fragments.
 */
object TerminalModeHolder {
    private val _selectedMode = MutableStateFlow(TerminalMode.SYNC)
    val selectedMode: StateFlow<TerminalMode> = _selectedMode.asStateFlow()

    fun setMode(mode: TerminalMode) {
        _selectedMode.value = mode
    }

    /** For Java interop: get current mode value. */
    fun getCurrentMode(): TerminalMode = _selectedMode.value
}

/** Map TerminalMode to ActivityMode for BPM range. */
fun TerminalMode.toActivityMode(): ActivityMode = when (this) {
    TerminalMode.ZEN -> ActivityMode.CALM
    TerminalMode.SYNC -> ActivityMode.DRIVING
    TerminalMode.OVERDRIVE -> ActivityMode.EXERCISE
}
