package com.heartbeatmusic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.heartbeatmusic.data.model.Achievement

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,   // e.g. "2026-03"
    val year: Int,
    val month: Int,               // 1-12
    val completedCount: Int,
    val totalCount: Int
) {
    fun toAchievement() = Achievement(
        id = id,
        year = year,
        month = month,
        completedCount = completedCount,
        totalCount = totalCount
    )
}

fun Achievement.toEntity() = AchievementEntity(
    id = id,
    year = year,
    month = month,
    completedCount = completedCount,
    totalCount = totalCount
)
