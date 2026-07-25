package com.parrotworks.redreamer.di

import android.content.Context
import androidx.room.Room
import com.parrotworks.redreamer.data.AppDatabase
import com.parrotworks.redreamer.data.DreamDao
import com.parrotworks.redreamer.data.DreamFtsDao
import com.parrotworks.redreamer.data.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            // Pre-release: no real user data to preserve yet, so a schema bump just
            // recreates the database instead of needing a hand-written Migration.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDreamDao(database: AppDatabase): DreamDao = database.dreamDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideDreamFtsDao(database: AppDatabase): DreamFtsDao = database.dreamFtsDao()
}
