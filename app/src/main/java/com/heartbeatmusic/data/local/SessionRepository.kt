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
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        dao.deleteById(sessionId)
        remote.deleteSession(sessionId)
            .onFailure { e: Throwable -> Log.w(TAG, "Background sync deleteSession failed", e) }
    }

    /** Pull Firestore sessions once and replace local DB (e.g. on app start). */
    suspend fun syncFromFirestore() = withContext(Dispatchers.IO) {
        // Read once from Firestore via the existing snapshot listener flow.
        // This avoids tight coupling to a specific one-shot API and keeps behavior consistent.
        val list = remote.sessionsFlow().first()
        dao.deleteAll()
        list.forEach { session: SyncSession -> dao.upsert(session.toEntity()) }
    }
}

