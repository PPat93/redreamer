package com.parrotworks.redreamer.di

import android.content.Context
import androidx.room.Room
import com.parrotworks.redreamer.data.ALL_MIGRATIONS
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
            // No destructive fallback on purpose. Wiping the journal to resolve a schema mismatch
            // is unacceptable now that real dreams live here; every version bump must ship a
            // migration in ALL_MIGRATIONS instead. If one is ever missing the app fails loudly at
            // startup, which is recoverable — a silent wipe is not.
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides
    fun provideDreamDao(database: AppDatabase): DreamDao = database.dreamDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideDreamFtsDao(database: AppDatabase): DreamFtsDao = database.dreamFtsDao()
}
