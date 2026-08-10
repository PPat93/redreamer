package com.parrotworks.redreamer.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Every schema change needs a migration here — the database deliberately has no destructive
 * fallback, because dropping it would silently destroy the user's entire journal.
 *
 * The SQL must match what Room generates for the target version *exactly*, otherwise Room's
 * post-migration validation fails. Copy it from the matching file in `app/schemas/`, which is
 * exported on every build and committed for this reason.
 */

/** Adds the FTS4 search index, backfilled from the dreams that already exist. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE VIRTUAL TABLE IF NOT EXISTS `dream_fts` " +
                "USING FTS4(`title` TEXT NOT NULL, `content` TEXT NOT NULL, `notes` TEXT NOT NULL)",
        )
        // Binned dreams are excluded from search, so they're excluded from the index too —
        // restoring one re-inserts its row.
        db.execSQL(
            "INSERT INTO `dream_fts`(`rowid`, `title`, `content`, `notes`) " +
                "SELECT `id`, `title`, `content`, `notes` FROM `dreams` WHERE `deletedAt` IS NULL",
        )
    }
}

/** Passed to Room in order; add each new migration here as the schema version climbs. */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
