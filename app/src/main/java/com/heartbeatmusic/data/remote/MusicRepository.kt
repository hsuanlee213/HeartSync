package com.heartbeatmusic.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.heartbeatmusic.data.model.ActivityMode
import com.heartbeatmusic.data.model.Song

class MusicRepository {

    private val db = FirebaseFirestore.getInstance()

    interface SongsCallback {
        fun onSuccess(songs: List<Song>?)
        fun onError(e: Exception?)
    }

    interface ActivityModesCallback {
        fun onSuccess(modes: List<ActivityMode>?)
        fun onError(e: Exception?)
    }

    fun getAllSongs(callback: SongsCallback) {
        db.collection(FirestoreCollections.SONGS)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val result = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Song::class.java)?.also { it.id = doc.id }
                }
                callback.onSuccess(result)
            }
            .addOnFailureListener { callback.onError(it) }
    }

    fun getAllActivityModes(callback: ActivityModesCallback) {
        db.collection(FirestoreCollections.ACTIVITY_MODES)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val result = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(ActivityMode::class.java)?.also { it.id = doc.id }
                }
                callback.onSuccess(result)
            }
            .addOnFailureListener { callback.onError(it) }
    }

    fun getSongsForMode(modeTag: String, callback: SongsCallback) {
        db.collection(FirestoreCollections.SONGS)
            .whereArrayContains("tags", modeTag)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val result = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Song::class.java)?.also { it.id = doc.id }
                }
                callback.onSuccess(result)
            }
            .addOnFailureListener { callback.onError(it) }
    }
}
