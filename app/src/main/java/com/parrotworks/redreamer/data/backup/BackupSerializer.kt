package com.parrotworks.redreamer.data.backup

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/** JSON encoding/decoding for backups, kept free of Android dependencies so it can be tested directly. */
@Singleton
class BackupSerializer @Inject constructor() {

    private val json = Json {
        prettyPrint = true
        // Tolerate files written by a newer build that added fields we don't know about yet.
        ignoreUnknownKeys = true
    }

    // Passing the generated serializer explicitly avoids relying on the reified extension,
    // which needs an extra import to resolve and otherwise picks the wrong overload.
    fun encode(backup: BackupFile): String = json.encodeToString(BackupFile.serializer(), backup)

    fun decode(raw: String): BackupFile = json.decodeFromString(BackupFile.serializer(), raw)
}
