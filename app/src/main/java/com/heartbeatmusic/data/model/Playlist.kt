package com.heartbeatmusic.data.model

// No-arg constructor required for Firestore deserialization
class Playlist {
    var id: String? = null
    var modeId: String? = null
    var name: String? = null
    var songIds: List<String>? = null
    var createdAt: Long = 0L
    var updatedAt: Long = 0L
}
