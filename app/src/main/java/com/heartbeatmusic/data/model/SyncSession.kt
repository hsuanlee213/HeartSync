package com.heartbeatmusic.data.model

/**
 * A playback session saved when user clicks STOP.
 */
data class SyncSession(
    val id: String,
    val userId: String,
    val mode: String, // ZEN, SYNC, OVERDRIVE
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationMinutes: Int,
    val songIds: List<String> = emptyList(),
    val songTitles: List<String> = emptyList()
)
