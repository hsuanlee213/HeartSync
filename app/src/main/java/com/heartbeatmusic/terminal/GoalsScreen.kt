package com.heartbeatmusic.terminal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heartbeatmusic.data.model.Achievement
import com.heartbeatmusic.data.model.DailyGoal
import java.util.Locale

private val GoalsBg = Color(0xFF1A1A2E)
private val CyanAccent = Color(0xFF00FFFF)
private val UnselectedGray = Color(0xFFB3B3B3)
private val CardBg = Color(0xFF252540)
private val CyanBorder = Color.Cyan.copy(alpha = 0.3f)

private fun modeIcon(mode: String): ImageVector = when (mode.uppercase()) {
    "ZEN" -> Icons.Default.Spa
    "OVERDRIVE" -> Icons.Default.Bolt
    else -> Icons.Default.Sync
}

private fun formatTimer(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(Locale.US, m, s)
}

@Composable
fun GoalsScreen(viewModel: GoalsViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val todayGoals by viewModel.todayGoals.collectAsStateWithLifecycle(initialValue = emptyList())
    val achievements by viewModel.achievements.collectAsStateWithLifecycle(initialValue = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GoalsBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GoalsTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(200)))
                },
                label = "GoalsTab"
            ) { tab ->
                when (tab) {
                    0 -> DailyGoalsContent(goals = todayGoals)
                    else -> AchievementsContent(achievements = achievements)
                }
            }
        }
    }
}

@Composable
private fun GoalsTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        listOf("DAILY GOALS", "ACHIEVEMENTS").forEachIndexed { index, label ->
            val isSelected = selectedTab == index
            GoalsTab(
                label = label,
                isSelected = isSelected,
                onClick = { onTabSelected(index) }
            )
        }
    }
}

@Composable
private fun GoalsTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) CyanAccent else UnselectedGray,
            modifier = Modifier
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(2.dp)
                    .background(CyanAccent)
            )
        }
    }
}

@Composable
private fun DailyGoalsContent(goals: List<DailyGoal>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(goals, key = { it.id }) { goal ->
            DailyGoalCard(goal = goal)
        }
    }
}

@Composable
private fun DailyGoalCard(goal: DailyGoal) {
    val progress = if (goal.targetMinutes > 0) {
        (goal.accumulatedSeconds.toFloat() / (goal.targetMinutes * 60)).coerceIn(0f, 1f)
    } else 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = modeIcon(goal.mode),
                    contentDescription = goal.mode,
                    tint = CyanAccent,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = goal.mode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            if (goal.isCompleted) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Completed",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${goal.targetMinutes} min",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = UnselectedGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CyanBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(CyanAccent)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatTimer(goal.accumulatedSeconds),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent
            )
        }
    }
}

@Composable
private fun AchievementsContent(achievements: List<Achievement>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(achievements, key = { it.id }) { achievement ->
            AchievementCard(achievement = achievement)
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    val monthName = java.util.Calendar.getInstance().apply {
        set(achievement.year, achievement.month - 1, 1)
    }.getDisplayName(
        java.util.Calendar.MONTH,
        java.util.Calendar.LONG,
        Locale.getDefault()
    ) ?: "${achievement.month}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Text(
            text = "$monthName ${achievement.year}",
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${achievement.completedCount} / ${achievement.totalCount} goals",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = CyanAccent
        )
    }
}
