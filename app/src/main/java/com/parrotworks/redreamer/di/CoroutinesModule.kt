package com.parrotworks.redreamer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * A scope that outlives any single screen. Needed for work that must finish even though the thing
 * that started it is going away — flushing a half-typed dream when the editor closes, above all.
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        // SupervisorJob so one failed background write can't tear down the rest.
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
