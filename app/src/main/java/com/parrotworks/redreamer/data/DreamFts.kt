package com.parrotworks.redreamer.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * A manually-synced FTS4 index over live dreams only. Not tied to [Dream] via `contentEntity`
 * (which would need hand-written SQLite triggers to stay in sync) — instead [dreamId] IS the
 * FTS table's rowid (the standard way to correlate an FTS4 row back to its source row), and
 * [com.parrotworks.redreamer.repository.DreamRepository] keeps rows here in lockstep with every
 * dream create/edit/soft-delete/restore.
 */
@Entity(tableName = "dream_fts")
@Fts4
data class DreamFts(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val dreamId: Long,
    val title: String,
    val content: String,
    val notes: String,
)
