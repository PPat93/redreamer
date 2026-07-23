package com.parrotworks.redreamer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * [deletedAt] null means the dream is live; a timestamp means it is in the bin
 * and gets purged 30 days after that timestamp. Sorting is always by
 * [dreamDate] (editable, defaults to the creation date) with [createdAt] as
 * the tiebreak for same-day entries — [updatedAt] is never a sort key so
 * editing a dream never moves it in the list.
 */
@Entity(tableName = "dreams")
data class Dream(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val notes: String = "",
    val dreamDate: LocalDate,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val isLucid: Boolean = false,
    val lucidity: Int? = null,
    val clarity: Int = 5,
    val isNightmare: Boolean = false,
    val isRecurring: Boolean = false,
    val moods: Set<Mood> = emptySet(),
)
