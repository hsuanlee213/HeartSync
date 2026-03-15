package com.heartbeatmusic

data class Track(
    val id: String,
    val title: String,
    val artist: String? = null,
    val coverUrl: String? = null,
    val url: String,
    var bpm: Int = 0
)
