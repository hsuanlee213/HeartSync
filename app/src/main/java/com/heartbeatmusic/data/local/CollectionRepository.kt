package com.heartbeatmusic.data.local

import android.content.Context
import android.util.Log
import com.heartbeatmusic.data.model.CollectionItem
import com.heartbeatmusic.data.remote.ArchiveRepository
import com.heartbeatmusic.data.remote.JamendoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val TAG = "CollectionRepository"

/**
 * Local-first repository for user's collection (favorites). Reads from Room;
 * writes go to Room first (UI updates immediately) then sync to Firestore in background.
 */
class CollectionRepository(
    private val context: Context,
    private val remote: ArchiveRepository,
    private val jamendo: JamendoRepository
) {
    private val dao = AppDatabase.getInstance(context).collectionDao()

    private val _collectionChanged = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    /** Emits when add or remove completes. HeartSyncViewModel subscribes to keep heart icon in sync. */
    val collectionChanged: SharedFlow<Unit> = _collectionChanged

    /** Flow of collection from local DB. UI observes this for instant updates. */
    fun collectionFlow(): Flow<List<CollectionItem>> =
        dao.getAllFlow().map { list -> list.map { it.toCollectionItem() } }

    /** Add item to local DB, then sync to Firestore on IO dispatcher. Local is source of truth.
     *  If song already exists in another mode, removes old entry first so each song appears only once. */
    suspend fun addToCollection(
        songId: String,
        title: String,
        artist: String,
        mode: String,
        coverUrl: String = "",
        addedAt: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        dao.deleteBySongId(songId)
        remote.removeAllBySongId(songId)
            .onFailure { Log.w(TAG, "removeAllBySongId failed (may not be logged in)", it) }
        val id = "${songId}_${mode}"
        dao.insert(
            CollectionItemEntity(
                id = id,
                songId = songId,
                title = title,
                artist = artist,
                mode = mode,
                coverUrl = coverUrl,
                addedAt = addedAt
            )
        )
        remote.addToCollection(songId, title, artist, mode, coverUrl, addedAt)
            .onFailure { Log.w(TAG, "Background sync add failed", it) }
        _collectionChanged.tryEmit(Unit)
    }

    /** Remove item from local DB, then sync to Firestore on IO dispatcher. */
    suspend fun removeFromCollection(songId: String, mode: String) = withContext(Dispatchers.IO) {
        dao.deleteBySongIdAndMode(songId, mode)
        remote.removeFromCollection(songId, mode)
            .onFailure { Log.w(TAG, "Background sync remove failed", it) }
        _collectionChanged.tryEmit(Unit)
    }

    /** Remove song from collection (any mode). Used when one song per collection. */
    suspend fun removeBySongId(songId: String) = withContext(Dispatchers.IO) {
        dao.deleteBySongId(songId)
        remote.removeAllBySongId(songId)
            .onFailure { Log.w(TAG, "removeAllBySongId failed", it) }
        _collectionChanged.tryEmit(Unit)
    }

    /**
     * Pull Firestore collection into local DB only when local is empty (e.g. new device, first login).
     * Do NOT overwrite local when it has data — local is source of truth.
     */
    suspend fun syncFromFirestore() = withContext(Dispatchers.IO) {
        val localItems = dao.getAllFlow().first()
        if (localItems.isNotEmpty()) return@withContext

        val list = remote.getCollectionOnce()
        val missingCoverIds = list.filter { it.coverUrl.isEmpty() }.map { it.songId }
        val coverMap = if (missingCoverIds.isNotEmpty()) {
            jamendo.fetchCoverUrls(missingCoverIds)
        } else {
            emptyMap()
        }

        val enriched = list.map { item ->
            if (item.coverUrl.isEmpty()) {
                val fetched = coverMap[item.songId] ?: ""
                if (fetched.isNotEmpty()) item.copy(coverUrl = fetched) else item
            } else {
                item
            }
        }

        enriched.forEach { item -> dao.insert(item.toEntity()) }

        enriched.filter { it.coverUrl.isNotEmpty() }.forEach { item ->
            remote.addToCollection(item.songId, item.title, item.artist, item.mode, item.coverUrl, item.addedAt)
                .onFailure { Log.w(TAG, "Cover URL backfill Firestore write failed for ${item.songId}", it) }
        }
    }
}
