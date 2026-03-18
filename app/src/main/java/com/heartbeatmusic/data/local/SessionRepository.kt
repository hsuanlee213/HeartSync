package com.heartbeatmusic.data.local

import android.content.Context
import android.util.Log
import com.heartbeatmusic.data.model.SyncSession
import com.heartbeatmusic.data.remote.ArchiveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val TAG = "SessionRepository"
private const val MAX_SESSIONS = 50

/**
 * Local-first repository for Archive sessions. Reads from Room;
 * writes go to Room first (UI updates immediately) then sync to Firestore in background.
 */
class SessionRepository(
    context: Context,
    private val remote: ArchiveRepository
) {
    private val dao = AppDatabase.getInstance(context).syncSessionDao()

    fun sessionsFlow(): Flow<List<SyncSession>> =
        dao.getAllFlow().map { list -> list.map { it.toSyncSession() } }

    suspend fun saveSession(session: SyncSession) = withContext(Dispatchers.IO) {
        dao.upsert(session.toEntity())
        remote.saveSession(session)
            .onFailure { e: Throwable -> Log.w(TAG, "Background sync saveSession failed", e) }
        trimOldSessions()
    }

    private suspend fun trimOldSessions() {
        val toDelete = dao.getIdsToTrim(MAX_SESSIONS)
        toDelete.forEach { id ->
            dao.deleteById(id)
            remote.deleteSession(id)
                .onFailure { e: Throwable -> Log.w(TAG, "Background sync trim deleteSession failed", e) }
        }
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        dao.deleteById(sessionId)
        remote.deleteSession(sessionId)
            .onFailure { e: Throwable -> Log.w(TAG, "Background sync deleteSession failed", e) }
    }

    /**
     * Pull Firestore sessions into local DB only when local is empty (e.g. new device, first login).
     * Do NOT overwrite local when it has data — local is source of truth; Firestore sync is async.
     */
    suspend fun syncFromFirestore() = withContext(Dispatchers.IO) {
        val localSessions = dao.getAllFlow().first()
        if (localSessions.isNotEmpty()) return@withContext
        val list = remote.sessionsFlow().first()
        list.forEach { session: SyncSession -> dao.upsert(session.toEntity()) }
    }
}

