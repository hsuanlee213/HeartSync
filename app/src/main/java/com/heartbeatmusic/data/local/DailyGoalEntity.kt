package com.heartbeatmusic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.heartbeatmusic.data.model.DailyGoal

@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey val id: String,       // e.g. "userId_2026-03-16_ZEN"
    val userId: String,
    val mode: String,                 // ZEN, SYNC, OVERDRIVE
    val targetMinutes: Int,
    val accumulatedSeconds: Int = 0,
    val date: String,                 // ISO date string, e.g. "2026-03-16"
    val isCompleted: Boolean = false
) {
    fun toDailyGoal() = DailyGoal(
        id = id,
        userId = userId,
        mode = mode,
        targetMinutes = targetMinutes,
        accumulatedSeconds = accumulatedSeconds,
        date = date,
        isCompleted = isCompleted
    )
}

fun DailyGoal.toEntity() = DailyGoalEntity(
    id = id,
    userId = userId,
    mode = mode,
    targetMinutes = targetMinutes,
    accumulatedSeconds = accumulatedSeconds,
    date = date,
    isCompleted = isCompleted
)
