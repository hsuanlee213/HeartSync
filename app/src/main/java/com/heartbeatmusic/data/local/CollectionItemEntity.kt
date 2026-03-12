package com.heartbeatmusic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.heartbeatmusic.data.model.CollectionItem

/**
 * Room entity for favorite (collection) items. Same shape as [CollectionItem];
 * used as local source of truth with background sync to Firestore.
 */
@Entity(tableName = "collection")
data class CollectionItemEntity(
    @PrimaryKey val id: String,
    val songId: String,
    val title: String,
    val artist: String,
    val mode: String,
    val coverUrl: String = ""
) {
    fun toCollectionItem() = CollectionItem(
        id = id,
        songId = songId,
        title = title,
        artist = artist,
        mode = mode,
        coverUrl = coverUrl
    )
}

fun CollectionItem.toEntity() = CollectionItemEntity(
    id = id,
    songId = songId,
    title = title,
    artist = artist,
    mode = mode,
    coverUrl = coverUrl
)
