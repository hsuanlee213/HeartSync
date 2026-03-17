package com.heartbeatmusic.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CollectionItemEntity::class,
        SyncSessionEntity::class,
        UserProfileEntity::class,
        DailyGoalEntity::class,
        AchievementEntity::class
    ],
    version = 8,  // v7 -> v8: add userId to DailyGoalEntity, AchievementEntity
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun syncSessionDao(): SyncSessionDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun dailyGoalDao(): DailyGoalDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "heartsync_db"
            )
                // v1 -> v2: add SyncSession table
                // v2 -> v3: add UserProfile table (avatar local/remote)
                // v7 -> v8: add userId to goals/achievements (dev: accepts full DB reset)
                .fallbackToDestructiveMigration()
                .build()
    }
}
