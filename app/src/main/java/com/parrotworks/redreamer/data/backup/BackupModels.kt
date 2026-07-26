package com.parrotworks.redreamer.data.backup

import kotlinx.serialization.Serializable

/**
 * On-disk backup format. Dates are ISO-8601 strings and tags are inlined by name so the file stays
 * human-readable and portable — importing re-creates/re-links tags by name rather than trusting
 * database ids from another install. [version] lets future releases migrate older files.
 */
@Serializable
data class BackupFile(
    val version: Int = CURRENT_VERSION,
    val exportedAt: String,
    /** Every known tag name, including ones not attached to any dream, so standalone tags survive. */
    val tags: List<String> = emptyList(),
    val dreams: List<BackupDream> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class BackupDream(
    val title: String = "",
    val content: String = "",
    val notes: String = "",
    /** ISO-8601 local date, e.g. "2026-07-24". */
    val dreamDate: String,
    /** ISO-8601 instant, e.g. "2026-07-24T06:12:00Z". */
    val createdAt: String,
    val updatedAt: String,
    val isLucid: Boolean = false,
    val lucidity: Int? = null,
    val clarity: Int = 5,
    val isNightmare: Boolean = false,
    val isRecurring: Boolean = false,
    val moods: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
)
