package com.heartbeatmusic.data.model

/**
 * A daily mission assigned to the user.
 * Two goals are generated per day, each targeting a specific mode and duration.
 */
data class DailyGoal(
    val id: String,           // e.g. "2026-03-16_ZEN"
    val mode: String,         // ZEN, SYNC, OVERDRIVE
    val targetMinutes: Int,   // 10, 15, 20, 25, or 30
    val accumulatedSeconds: Int = 0,
    val date: String,         // ISO date string, e.g. "2026-03-16"
    val isCompleted: Boolean = false
)
