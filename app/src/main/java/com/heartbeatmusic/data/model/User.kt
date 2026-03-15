package com.heartbeatmusic.data.model

// No-arg constructor required for Firestore deserialization
class User {
    var id: String? = null
    var displayName: String? = null
    var email: String? = null
    var createdAt: Long = 0L
}
