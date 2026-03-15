package com.heartbeatmusic.terminal

import androidx.compose.ui.graphics.Color

/**
 * App mode for Terminal. Each mode has predefined music tags, accent color, and BPM range.
 */
enum class TerminalMode(
    val musicTags: String,
    val accentColor: Color,
    val mockTitle: String,
    val mockArtist: String,
    val minBpm: Int,
    val maxBpm: Int
) {
    ZEN(
        musicTags = "ambient, meditation, piano, soft, peaceful",
        accentColor = Color(0xFF39FF14), // Fluorescent green (matches heart)
        mockTitle = "Eternal Peace",
        mockArtist = "Calm Master",
        minBpm = 50,
        maxBpm = 80
    ),
    SYNC(
        musicTags = "deep house, lofi, focus, steady, pop",
        accentColor = Color(0xFF00FFFF), // Cyan
        mockTitle = "Digital Pulse",
        mockArtist = "Sync Theory",
        minBpm = 80,
        maxBpm = 120
    ),
    OVERDRIVE(
        musicTags = "techno, rock, aggressive, powerful, high-tempo",
        accentColor = Color(0xFFFFD700), // Gold (matches heart)
        mockTitle = "System Overload",
        mockArtist = "Kinetic Power",
        minBpm = 120,
        maxBpm = 160
    )
}

