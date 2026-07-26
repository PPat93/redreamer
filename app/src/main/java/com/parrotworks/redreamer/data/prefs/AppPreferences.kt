package com.parrotworks.redreamer.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "redreamer_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_APP_LOCK] ?: false }

    val autoBackupEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_BACKUP] ?: true }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_APP_LOCK] = enabled }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_BACKUP] = enabled }
    }

    suspend fun lastAutoBackupAtMillis(): Long =
        context.dataStore.data.first()[KEY_LAST_AUTO_BACKUP] ?: 0L

    suspend fun setLastAutoBackupAtMillis(millis: Long) {
        context.dataStore.edit { it[KEY_LAST_AUTO_BACKUP] = millis }
    }

    private companion object {
        val KEY_APP_LOCK = booleanPreferencesKey("app_lock_enabled")
        val KEY_AUTO_BACKUP = booleanPreferencesKey("auto_backup_enabled")
        val KEY_LAST_AUTO_BACKUP = longPreferencesKey("last_auto_backup_at")
    }
}
