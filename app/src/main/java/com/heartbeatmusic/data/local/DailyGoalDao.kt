package com.heartbeatmusic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: DailyGoalEntity)

    @Update
    suspend fun update(entity: DailyGoalEntity)

    @Query("SELECT * FROM daily_goals WHERE date = :date")
    fun getByDateFlow(date: String): Flow<List<DailyGoalEntity>>

    @Query("SELECT * FROM daily_goals WHERE date = :date")
    suspend fun getByDate(date: String): List<DailyGoalEntity>

    @Query("SELECT * FROM daily_goals WHERE date LIKE :yearMonth || '%'")
    suspend fun getByMonth(yearMonth: String): List<DailyGoalEntity>
}
