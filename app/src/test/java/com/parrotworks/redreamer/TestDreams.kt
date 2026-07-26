package com.parrotworks.redreamer

import com.parrotworks.redreamer.data.Dream
import com.parrotworks.redreamer.data.DreamWithTags
import com.parrotworks.redreamer.data.Mood
import com.parrotworks.redreamer.data.Tag
import java.time.Instant
import java.time.LocalDate

/** Builds a dream with sensible defaults so each test only states the fields it cares about. */
fun dream(
    id: Long = 1,
    title: String = "A dream",
    content: String = "Something happened",
    notes: String = "",
    dreamDate: LocalDate = LocalDate.of(2026, 7, 1),
    createdAt: Instant = Instant.parse("2026-07-01T06:00:00Z"),
    isLucid: Boolean = false,
    lucidity: Int? = null,
    clarity: Int = 5,
    isNightmare: Boolean = false,
    isRecurring: Boolean = false,
    moods: Set<Mood> = emptySet(),
    tags: List<String> = emptyList(),
): DreamWithTags = DreamWithTags(
    dream = Dream(
        id = id,
        title = title,
        content = content,
        notes = notes,
        dreamDate = dreamDate,
        createdAt = createdAt,
        updatedAt = createdAt,
        isLucid = isLucid,
        lucidity = lucidity,
        clarity = clarity,
        isNightmare = isNightmare,
        isRecurring = isRecurring,
        moods = moods,
    ),
    tags = tags.mapIndexed { index, name -> Tag(id = index + 1L, name = name) },
)
