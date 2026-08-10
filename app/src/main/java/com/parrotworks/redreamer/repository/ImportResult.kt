package com.parrotworks.redreamer.repository

/**
 * Outcome of importing a backup file. [skipped] counts entries that couldn't be read — surfacing it
 * matters because a silently shortened import looks identical to a successful one.
 */
data class ImportResult(val imported: Int, val skipped: Int)
