package com.heartbeatmusic.data.model

// No-arg constructor required for Firestore deserialization (doc.toObject(Song::class.java))
class Song {
    var id: String? = null
    var title: String? = null
    var artist: String? = null
    var bpm: Long? = null
    var durationSec: Int = 0
    var genre: String? = null
    var tags: List<String>? = null
    var audioSourceType: String? = null
    var localFileId: String? = null
    var audioUrl: String? = null
    var coverUrl: String? = null
}
