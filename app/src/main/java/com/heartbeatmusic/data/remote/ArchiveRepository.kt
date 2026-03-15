package com.heartbeatmusic.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.heartbeatmusic.data.model.CollectionItem
import com.heartbeatmusic.data.model.SyncSession
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ArchiveRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun currentUserId(): String? = auth.currentUser?.uid

    suspend fun saveSession(session: SyncSession): Result<Unit> = runCatching {
        db.collection(FirestoreCollections.SYNC_SESSIONS)
            .document(session.id)
            .set(
                mapOf(
                    "userId" to session.userId,
                    "mode" to session.mode,
                    "startTimestamp" to session.startTimestamp,
                    "endTimestamp" to session.endTimestamp,
                    "durationMinutes" to session.durationMinutes,
                    "songIds" to session.songIds,
                    "songTitles" to session.songTitles,
                    "songArtists" to session.songArtists
                )
            )
            .await()
    }

    /** Fetch sessions once (for initial sync from Firestore into local DB). */
    suspend fun getSessionsOnce(): List<SyncSession> = runCatching {
        val uid = currentUserId() ?: return@runCatching emptyList()
        val snapshot = db.collection(FirestoreCollections.SYNC_SESSIONS)
            .whereEqualTo("userId", uid)
            .get()
            .await()
        snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            SyncSession(
                id = doc.id,
                userId = (data["userId"] as? String) ?: "",
                mode = (data["mode"] as? String) ?: "SYNC",
                startTimestamp = (data["startTimestamp"] as? Number)?.toLong() ?: 0L,
                endTimestamp = (data["endTimestamp"] as? Number)?.toLong() ?: 0L,
                durationMinutes = (data["durationMinutes"] as? Number)?.toInt() ?: 0,
                songIds = (data["songIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                songTitles = (data["songTitles"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                songArtists = (data["songArtists"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            )
        }.sortedByDescending { it.endTimestamp }
    }.getOrElse { emptyList() }

    fun sessionsFlow(): Flow<List<SyncSession>> = callbackFlow {
        val uid = currentUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection(FirestoreCollections.SYNC_SESSIONS)
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    SyncSession(
                        id = doc.id,
                        userId = (data["userId"] as? String) ?: "",
                        mode = (data["mode"] as? String) ?: "SYNC",
                        startTimestamp = (data["startTimestamp"] as? Number)?.toLong() ?: 0L,
                        endTimestamp = (data["endTimestamp"] as? Number)?.toLong() ?: 0L,
                        durationMinutes = (data["durationMinutes"] as? Number)?.toInt() ?: 0,
                        songIds = (data["songIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        songTitles = (data["songTitles"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        songArtists = (data["songArtists"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    )
                } ?: emptyList()
                trySend(list.sortedByDescending { it.endTimestamp })
            }
        awaitClose { listener.remove() }
    }

    fun collectionFlow(): Flow<List<CollectionItem>> = callbackFlow {
        val uid = currentUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection(FirestoreCollections.USERS)
            .document(uid)
            .collection(FirestoreCollections.COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    CollectionItem(
                        id = doc.id,
                        songId = (data["songId"] as? String) ?: "",
                        title = (data["title"] as? String) ?: "",
                        artist = (data["artist"] as? String) ?: "",
                        mode = (data["mode"] as? String) ?: "SYNC",
                        coverUrl = (data["coverUrl"] as? String) ?: ""
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Composite document ID: songId_mode. Allows same song in ZEN/SYNC/OVERDRIVE separately. */
    private fun collectionDocId(songId: String, mode: String) = "${songId}_${mode}"

    /** Fetch collection once (for initial sync from Firestore into local DB). */
    suspend fun getCollectionOnce(): List<CollectionItem> = runCatching {
        val uid = currentUserId() ?: return@runCatching emptyList()
        val snapshot = db.collection(FirestoreCollections.USERS)
            .document(uid)
            .collection(FirestoreCollections.COLLECTION)
            .get()
            .await()
        snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            CollectionItem(
                id = doc.id,
                songId = (data["songId"] as? String) ?: "",
                title = (data["title"] as? String) ?: "",
                artist = (data["artist"] as? String) ?: "",
                mode = (data["mode"] as? String) ?: "SYNC",
                coverUrl = (data["coverUrl"] as? String) ?: ""
            )
        }
    }.getOrElse { emptyList() }

    suspend fun addToCollection(songId: String, title: String, artist: String, mode: String, coverUrl: String = ""): Result<Unit> = runCatching {
        val uid = currentUserId() ?: throw IllegalStateException("Not logged in")
        val docId = collectionDocId(songId, mode)
        db.collection(FirestoreCollections.USERS)
            .document(uid)
            .collection(FirestoreCollections.COLLECTION)
            .document(docId)
            .set(
                mapOf(
                    "songId" to songId,
                    "title" to title,
                    "artist" to artist,
                    "mode" to mode,
                    "coverUrl" to coverUrl
                )
            )
            .await()
    }

    suspend fun deleteSession(sessionId: String): Result<Unit> = runCatching {
        db.collection(FirestoreCollections.SYNC_SESSIONS)
            .document(sessionId)
            .delete()
            .await()
    }

    /** Delete only the entry for songId + mode. WHERE songId = :id AND mode = :mode. */
    suspend fun removeFromCollection(songId: String, mode: String): Result<Unit> = runCatching {
        val uid = currentUserId() ?: throw IllegalStateException("Not logged in")
        val docs = db.collection(FirestoreCollections.USERS)
            .document(uid)
            .collection(FirestoreCollections.COLLECTION)
            .whereEqualTo("songId", songId)
            .whereEqualTo("mode", mode)
            .get()
            .await()
        docs.documents.forEach { it.reference.delete().await() }
    }
}
