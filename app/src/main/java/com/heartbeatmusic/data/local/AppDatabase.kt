package com.heartbeatmusic.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CollectionItemEntity::class,
        SyncSessionEntity::class,
        UserProfileEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun syncSessionDao(): SyncSessionDao
    abstract fun userProfileDao(): UserProfileDao

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
                "heartbeat_db"
            )
                // v1 -> v2: add SyncSession table
                // v2 -> v3: add UserProfile table (avatar local/remote)
                .fallbackToDestructiveMigration()
                .build()
    }
}
