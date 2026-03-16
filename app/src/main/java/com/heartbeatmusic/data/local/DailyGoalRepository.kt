package com.heartbeatmusic.data.local

import android.content.Context
import com.heartbeatmusic.data.model.DailyGoal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Local-first repository for daily goals. Room is the sole source of truth —
 * no remote sync needed for goals data.
 */
class DailyGoalRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).dailyGoalDao()

    /** Observe today's goals in real time for the given user. */
    fun todayGoalsFlow(userId: String, date: String): Flow<List<DailyGoal>> =
        dao.getByDateFlow(userId, date).map { list -> list.map { it.toDailyGoal() } }

    /** Check whether goals already exist for a given date and user. */
    suspend fun getGoalsByDate(userId: String, date: String): List<DailyGoal> = withContext(Dispatchers.IO) {
        dao.getByDate(userId, date).map { it.toDailyGoal() }
    }

    /** Insert a new goal (ignored if id already exists). */
    suspend fun insertGoal(goal: DailyGoal) = withContext(Dispatchers.IO) {
        dao.insert(goal.toEntity())
    }

    /** Persist updated accumulatedSeconds or isCompleted for an existing goal. */
    suspend fun updateGoal(goal: DailyGoal) = withContext(Dispatchers.IO) {
        dao.update(goal.toEntity())
    }

    /** Fetch all goals belonging to a given month and user (yearMonth = "2026-03"). */
    suspend fun getGoalsByMonth(userId: String, yearMonth: String): List<DailyGoal> = withContext(Dispatchers.IO) {
        dao.getByMonth(userId, yearMonth).map { it.toDailyGoal() }
    }
}
