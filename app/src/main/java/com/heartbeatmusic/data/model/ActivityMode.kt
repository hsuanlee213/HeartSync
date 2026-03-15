package com.heartbeatmusic.data.model

// No-arg constructor required for Firestore deserialization
class ActivityMode {
    var id: String? = null
    var name: String? = null
    var description: String? = null
    var minBpm: Int = 0
    var maxBpm: Int = 0
    var baseGenres: List<String>? = null
    var createdAt: Long = 0L
}
