package com.heartbeatmusic.data.local

import android.content.Context
import com.heartbeatmusic.data.model.Achievement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Local-first repository for monthly achievements. Room is the sole source of truth.
 */
class AchievementRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).achievementDao()

    /** Observe all achievement records for the given user, ordered by most recent month. */
    fun achievementsFlow(userId: String): Flow<List<Achievement>> =
        dao.getAllFlow(userId).map { list -> list.map { it.toAchievement() } }

    /** Insert or replace an achievement record for a given month. */
    suspend fun insertAchievement(achievement: Achievement) = withContext(Dispatchers.IO) {
        dao.insert(achievement.toEntity())
    }
}
