package com.heartbeatmusic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AchievementEntity)

    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY year DESC, month DESC")
    fun getAllFlow(userId: String): Flow<List<AchievementEntity>>
}
