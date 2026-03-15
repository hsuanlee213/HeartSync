package com.heartbeatmusic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.heartbeatmusic.data.model.SyncSession
import org.json.JSONArray

/**
 * Room entity for Archive sessions. Stored locally as the source of truth,
 * then synchronized to Firestore in the background.
 *
 * songIdsJson / songTitlesJson are stored as JSON arrays (e.g. ["id1","id2"])
 * so that any special characters in song titles are safely escaped.
 */
@Entity(tableName = "sync_sessions")
data class SyncSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val mode: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationMinutes: Int,
    val songIdsCsv: String = "[]",
    val songTitlesCsv: String = "[]"
) {
    fun toSyncSession(): SyncSession = SyncSession(
        id = id,
        userId = userId,
        mode = mode,
        startTimestamp = startTimestamp,
        endTimestamp = endTimestamp,
        durationMinutes = durationMinutes,
        songIds = jsonToList(songIdsCsv),
        songTitles = jsonToList(songTitlesCsv)
    )
}

fun SyncSession.toEntity(): SyncSessionEntity = SyncSessionEntity(
    id = id,
    userId = userId,
    mode = mode,
    startTimestamp = startTimestamp,
    endTimestamp = endTimestamp,
    durationMinutes = durationMinutes,
    songIdsCsv = listToJson(songIds),
    songTitlesCsv = listToJson(songTitles)
)

private fun listToJson(list: List<String>): String =
    JSONArray(list).toString()

private fun jsonToList(json: String): List<String> = runCatching {
    val arr = JSONArray(json)
    List(arr.length()) { arr.getString(it) }
}.getOrDefault(emptyList())

