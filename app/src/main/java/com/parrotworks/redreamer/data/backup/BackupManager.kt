package com.parrotworks.redreamer.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Moves [BackupFile] JSON between the app and either a user-picked SAF location or private storage. */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serializer: BackupSerializer,
) {
    suspend fun writeToUri(uri: Uri, backup: BackupFile) = withContext(Dispatchers.IO) {
        val payload = serializer.encode(backup)
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(payload.toByteArray())
        } ?: error("Could not open $uri for writing")
    }

    suspend fun readFromUri(uri: Uri): BackupFile = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().decodeToString()
        } ?: error("Could not open $uri for reading")
        serializer.decode(raw)
    }

    /**
     * Writes an app-private snapshot and prunes all but the newest [MAX_LOCAL_BACKUPS]. These live in
     * internal storage, so they guard against in-app data loss but are removed if the app is
     * uninstalled — device-level recovery is covered by Android Auto Backup instead.
     */
    suspend fun writeLocalSnapshot(backup: BackupFile, timestampLabel: String) = withContext(Dispatchers.IO) {
        val dir = localBackupDir()
        File(dir, "redreamer-backup-$timestampLabel.json").writeText(serializer.encode(backup))

        dir.listFiles { file -> file.isFile && file.name.endsWith(".json") }
            ?.sortedByDescending { it.name }
            ?.drop(MAX_LOCAL_BACKUPS)
            ?.forEach { it.delete() }
        Unit
    }

    private fun localBackupDir(): File = File(context.filesDir, "backups").apply { mkdirs() }

    companion object {
        const val MAX_LOCAL_BACKUPS = 5
        const val EXPORT_MIME_TYPE = "application/json"

        /**
         * Deliberately unrestricted. Storage providers are wildly inconsistent about the MIME type
         * they report for a .json file — "application/json", "text/plain" and
         * "application/octet-stream" are all common — and anything not matching the filter is shown
         * greyed out and unselectable. Accepting everything and validating the contents after
         * reading is the only way the user can reliably pick their own export back.
         */
        val IMPORT_MIME_TYPES = arrayOf("*/*")
    }
}
