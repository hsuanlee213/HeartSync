package com.heartbeatmusic.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartbeatmusic.data.local.AchievementRepository
import com.heartbeatmusic.data.local.DailyGoalRepository
import com.heartbeatmusic.data.model.Achievement
import com.heartbeatmusic.data.model.DailyGoal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.random.Random

private val TARGET_MINUTES_OPTIONS = listOf(10, 15, 20, 25, 30)
private val ALL_MODES = listOf("ZEN", "SYNC", "OVERDRIVE")
private val DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE  // "2026-03-16"

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val dailyGoalRepository: DailyGoalRepository,
    private val achievementRepository: AchievementRepository
) : ViewModel() {

    private val _todayGoals = MutableStateFlow<List<DailyGoal>>(emptyList())
    val todayGoals: StateFlow<List<DailyGoal>> = _todayGoals.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    init {
        val today = LocalDate.now().format(DATE_FORMAT)
        dailyGoalRepository.todayGoalsFlow(today)
            .onEach { _todayGoals.value = it }
            .launchIn(viewModelScope)
        achievementRepository.achievementsFlow()
            .onEach { _achievements.value = it }
            .launchIn(viewModelScope)
        viewModelScope.launch(Dispatchers.IO) {
            generateTodayGoalsIfNeeded(today)
            ensureLastMonthAchievementRecorded(today)
        }
    }

    /**
     * Step 6: On app launch, check if last month's achievement has been recorded.
     * Count completed/total goals for the previous month and insert or update AchievementEntity.
     */
    private suspend fun ensureLastMonthAchievementRecorded(today: String) {
        val lastMonth = LocalDate.parse(today).minusMonths(1)
        val yearMonth = "${lastMonth.year}-${lastMonth.monthValue.toString().padStart(2, '0')}"
        val goals = dailyGoalRepository.getGoalsByMonth(yearMonth)
        if (goals.isEmpty()) return
        val completed = goals.count { it.isCompleted }
        achievementRepository.insertAchievement(
            Achievement(
                id = yearMonth,
                year = lastMonth.year,
                month = lastMonth.monthValue,
                completedCount = completed,
                totalCount = goals.size
            )
        )
    }

    private suspend fun generateTodayGoalsIfNeeded(today: String) {
        val existing = dailyGoalRepository.getGoalsByDate(today)
        if (existing.isNotEmpty()) return

        val modes = ALL_MODES.shuffled().take(2)
        modes.forEach { mode ->
            val targetMinutes = TARGET_MINUTES_OPTIONS[Random.nextInt(TARGET_MINUTES_OPTIONS.size)]
            dailyGoalRepository.insertGoal(
                DailyGoal(
                    id = "${today}_${mode}",
                    mode = mode,
                    targetMinutes = targetMinutes,
                    accumulatedSeconds = 0,
                    date = today,
                    isCompleted = false
                )
            )
        }
    }
}
