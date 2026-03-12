package com.heartbeatmusic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.heartbeatmusic.data.model.SyncSession

/**
 * Room entity for Archive sessions. Stored locally as the source of truth,
 * then synchronized to Firestore in the background.
 */
@Entity(tableName = "sync_sessions")
data class SyncSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val mode: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationMinutes: Int,
    val songIdsCsv: String = "",
    val songTitlesCsv: String = ""
) {
    fun toSyncSession(): SyncSession = SyncSession(
        id = id,
        userId = userId,
        mode = mode,
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp,
        durationMinutes = durationMinutes,
        songIds = songIdsCsv.takeIf { it.isNotEmpty() }?.split("||") ?: emptyList(),
        songTitles = songTitlesCsv.takeIf { it.isNotEmpty() }?.split("||") ?: emptyList()
    )
}

fun SyncSession.toEntity(): SyncSessionEntity = SyncSessionEntity(
    id = id,
    userId = userId,
    mode = mode,
    startTimestamp = startTimestamp,
    endTimestamp = endTimestamp,
    durationMinutes = durationMinutes,
    songIdsCsv = songIds.joinToString("||"),
    songTitlesCsv = songTitles.joinToString("||")
)

