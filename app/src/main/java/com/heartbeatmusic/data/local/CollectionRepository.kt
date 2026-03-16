package com.heartbeatmusic.data.local

import android.content.Context
import android.util.Log
import com.heartbeatmusic.data.model.CollectionItem
import com.heartbeatmusic.data.remote.ArchiveRepository
import com.heartbeatmusic.data.remote.JamendoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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

    /** Flow of collection from local DB. UI observes this for instant updates. */
    fun collectionFlow(): Flow<List<CollectionItem>> =
        dao.getAllFlow().map { list -> list.map { it.toCollectionItem() } }

    /** Add item to local DB, then sync to Firestore on IO dispatcher. Local is source of truth. */
    suspend fun addToCollection(
        songId: String,
        title: String,
        artist: String,
        mode: String,
        coverUrl: String = ""
    ) = withContext(Dispatchers.IO) {
        val id = "${songId}_${mode}"
        dao.insert(
            CollectionItemEntity(
                id = id,
                songId = songId,
                title = title,
                artist = artist,
                mode = mode,
                coverUrl = coverUrl
            )
        )
        remote.addToCollection(songId, title, artist, mode, coverUrl)
            .onFailure { Log.w(TAG, "Background sync add failed", it) }
    }

    /** Remove item from local DB, then sync to Firestore on IO dispatcher. */
    suspend fun removeFromCollection(songId: String, mode: String) = withContext(Dispatchers.IO) {
        dao.deleteBySongIdAndMode(songId, mode)
        remote.removeFromCollection(songId, mode)
            .onFailure { Log.w(TAG, "Background sync remove failed", it) }
    }

    /** Pull Firestore collection once and replace local DB (e.g. on login or app start).
     *  Any items missing a coverUrl are backfilled via a single Jamendo batch lookup. */
    suspend fun syncFromFirestore() = withContext(Dispatchers.IO) {
        val list = remote.getCollectionOnce()

        // Backfill cover URLs for items that were saved before cover tracking was added.
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

        dao.deleteAll()
        enriched.forEach { item -> dao.insert(item.toEntity()) }

        // Persist backfilled cover URLs back to Firestore so future syncs don't need to re-fetch.
        enriched.filter { it.coverUrl.isNotEmpty() }.forEach { item ->
            remote.addToCollection(item.songId, item.title, item.artist, item.mode, item.coverUrl)
                .onFailure { Log.w(TAG, "Cover URL backfill Firestore write failed for ${item.songId}", it) }
        }
    }
}
