package com.heartbeatmusic.data.model

/**
 * Monthly achievement record.
 * Summarises how many daily goals the user completed in a given month.
 */
data class Achievement(
    val id: String,          // e.g. "userId_2026-03"
    val userId: String,
    val year: Int,
    val month: Int,          // 1-12
    val completedCount: Int,
    val totalCount: Int
)
