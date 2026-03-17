package com.heartbeatmusic.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.heartbeatmusic.data.local.AchievementRepository
import com.heartbeatmusic.data.local.AvatarRepository
import com.heartbeatmusic.data.local.CollectionRepository
import com.heartbeatmusic.data.local.DailyGoalRepository
import com.heartbeatmusic.data.local.EssentialAudioRepository
import com.heartbeatmusic.data.local.SessionRepository
import com.heartbeatmusic.data.remote.ArchiveRepository
import com.heartbeatmusic.data.remote.JamendoApiService
import com.heartbeatmusic.data.remote.JamendoRepository
import com.heartbeatmusic.data.remote.LibraryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideArchiveRepository(): ArchiveRepository = ArchiveRepository()

    @Provides
    @Singleton
    fun provideLibraryRepository(): LibraryRepository = LibraryRepository()

    @Provides
    @Singleton
    fun provideCollectionRepository(
        @ApplicationContext context: Context,
        archiveRepository: ArchiveRepository,
        jamendoRepository: JamendoRepository
    ): CollectionRepository = CollectionRepository(context, archiveRepository, jamendoRepository)

    @Provides
    @Singleton
    fun provideSessionRepository(
        @ApplicationContext context: Context,
        archiveRepository: ArchiveRepository
    ): SessionRepository = SessionRepository(context, archiveRepository)

    @Provides
    @Singleton
    fun provideAvatarRepository(
        @ApplicationContext context: Context
    ): AvatarRepository = AvatarRepository(context)

    @Provides
    @Singleton
    fun provideEssentialAudioRepository(
        @ApplicationContext context: Context
    ): EssentialAudioRepository = EssentialAudioRepository(context)

    @Provides
    @Singleton
    fun provideJamendoApiService(): JamendoApiService =
        Retrofit.Builder()
            .baseUrl("https://api.jamendo.com/v3.0/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JamendoApiService::class.java)

    @Provides
    @Singleton
    fun provideJamendoRepository(api: JamendoApiService): JamendoRepository =
        JamendoRepository(api)

    @Provides
    @Singleton
    fun provideDailyGoalRepository(
        @ApplicationContext context: Context
    ): DailyGoalRepository = DailyGoalRepository(context)

    @Provides
    @Singleton
    fun provideAchievementRepository(
        @ApplicationContext context: Context
    ): AchievementRepository = AchievementRepository(context)
}
