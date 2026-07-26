package com.parrotworks.redreamer.repository

import com.parrotworks.redreamer.data.backup.BackupManager
import com.parrotworks.redreamer.data.prefs.AppPreferences
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * App-launch housekeeping: purging expired bin entries and writing the periodic private snapshot.
 * Both are cheap, idempotent, and deliberately not background-scheduled — running them once per
 * launch avoids depending on WorkManager (and OEM battery managers that kill it).
 */
@Singleton
class MaintenanceRunner @Inject constructor(
    private val repository: DreamRepository,
    private val backupManager: BackupManager,
    private val preferences: AppPreferences,
) {
    private val labelFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault())

    suspend fun runStartupTasks() {
        repository.purgeExpiredFromBin()
        maybeWriteAutoBackup()
    }

    private suspend fun maybeWriteAutoBackup() {
        if (!preferences.autoBackupEnabled.first()) return

        val now = Instant.now()
        val lastRun = preferences.lastAutoBackupAtMillis()
        if (now.toEpochMilli() - lastRun < AUTO_BACKUP_INTERVAL_MS) return

        val snapshot = repository.exportSnapshot()
        if (snapshot.dreams.isEmpty()) return

        backupManager.writeLocalSnapshot(snapshot, labelFormatter.format(now))
        preferences.setLastAutoBackupAtMillis(now.toEpochMilli())
    }

    private companion object {
        const val AUTO_BACKUP_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }
}
