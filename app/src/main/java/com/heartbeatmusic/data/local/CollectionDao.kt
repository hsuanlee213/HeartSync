package com.heartbeatmusic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collection ORDER BY addedAt DESC")
    fun getAllFlow(): Flow<List<CollectionItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CollectionItemEntity)

    @Query("DELETE FROM collection WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM collection WHERE songId = :songId AND mode = :mode")
    suspend fun deleteBySongIdAndMode(songId: String, mode: String)

    @Query("DELETE FROM collection")
    suspend fun deleteAll()
}
