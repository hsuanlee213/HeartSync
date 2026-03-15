package com.heartbeatmusic.data.model

// No-arg constructor required for Firestore deserialization
class HeartRateLog {
    var id: String? = null
    var userId: String? = null
    var timestamp: Long = 0L
    var bpm: Int = 0
    var source: String? = null
    var modeId: String? = null
    var sessionId: String? = null
}
