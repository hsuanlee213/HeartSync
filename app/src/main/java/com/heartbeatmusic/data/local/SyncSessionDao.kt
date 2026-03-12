package com.heartbeatmusic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncSessionDao {

    @Query("SELECT * FROM sync_sessions ORDER BY endTimestamp DESC")
    fun getAllFlow(): Flow<List<SyncSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncSessionEntity)

    @Query("DELETE FROM sync_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sync_sessions")
    suspend fun deleteAll()
}

