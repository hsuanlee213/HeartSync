package com.heartbeatmusic.data.model

/**
 * A song saved to user's collection with mode tag.
 */
data class CollectionItem(
    val id: String,
    val songId: String,
    val title: String,
    val artist: String,
    val mode: String, // ZEN, SYNC, OVERDRIVE
    val coverUrl: String = ""
)
