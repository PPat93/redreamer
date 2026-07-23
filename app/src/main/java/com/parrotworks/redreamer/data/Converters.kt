package com.parrotworks.redreamer.data

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromMoodSet(value: Set<Mood>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun toMoodSet(value: String): Set<Mood> =
        if (value.isBlank()) {
            emptySet()
        } else {
            value.split(",").map(Mood::valueOf).toSet()
        }
}
