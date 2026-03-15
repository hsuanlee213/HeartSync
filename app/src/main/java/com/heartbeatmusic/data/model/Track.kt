package com.heartbeatmusic.data.model

data class Track(
    var id: String? = null,
    var title: String? = null,
    var artist: String? = null,
    var coverUrl: String? = null,
    var url: String? = null,
    var bpm: Int = 0
)
