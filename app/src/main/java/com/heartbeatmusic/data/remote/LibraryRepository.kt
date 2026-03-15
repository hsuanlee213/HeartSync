package com.heartbeatmusic.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.heartbeatmusic.data.model.ActivityMode
import com.heartbeatmusic.data.model.Song

class LibraryRepository {

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
                val baseSongs = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Song::class.java)?.also { it.id = doc.id }
                }.toMutableList()

                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    callback.onSuccess(baseSongs)
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .document(user.uid)
                    .collection("songSettings")
                    .get()
                    .addOnSuccessListener { settingsSnapshot ->
                        val bpmOverrides = settingsSnapshot.documents.associate { doc ->
                            doc.id to (doc.get("bpm") as? Number)?.toInt()
                        }
                        baseSongs.forEach { song ->
                            bpmOverrides[song.id]?.let { song.bpm = it.toLong() }
                        }
                        callback.onSuccess(baseSongs)
                    }
                    .addOnFailureListener { callback.onError(it) }
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
}
