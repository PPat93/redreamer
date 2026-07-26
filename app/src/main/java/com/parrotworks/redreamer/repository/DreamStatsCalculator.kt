package com.parrotworks.redreamer.repository

import com.parrotworks.redreamer.data.DreamWithTags
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Pure derivation of [DreamStats] from a list of dreams. [today] is a parameter rather than a call
 * to [LocalDate.now] so results are deterministic and testable.
 */
object DreamStatsCalculator {

    const val TOP_TAGS_LIMIT = 8
    const val MONTHS_BACK = 6

    fun calculate(dreams: List<DreamWithTags>, today: LocalDate = LocalDate.now()): DreamStats {
        val total = dreams.size
        if (total == 0) return DreamStats.EMPTY

        val topTags = dreams.flatMap { it.tags }
            .groupingBy { it.name }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(TOP_TAGS_LIMIT)
            .map { TagCount(it.key, it.value) }

        val currentMonth = YearMonth.from(today)
        val dreamsPerMonth = (MONTHS_BACK - 1 downTo 0).map { offset ->
            val month = currentMonth.minusMonths(offset.toLong())
            MonthCount(month, dreams.count { YearMonth.from(it.dream.dreamDate) == month })
        }

        return DreamStats(
            totalDreams = total,
            lucidPercent = dreams.count { it.dream.isLucid } * 100 / total,
            nightmarePercent = dreams.count { it.dream.isNightmare } * 100 / total,
            recurringPercent = dreams.count { it.dream.isRecurring } * 100 / total,
            averageClarity = dreams.map { it.dream.clarity }.average(),
            moodCounts = dreams.flatMap { it.dream.moods }.groupingBy { it }.eachCount(),
            topTags = topTags,
            dreamsPerMonth = dreamsPerMonth,
            currentStreakDays = currentStreak(dreams.map { it.dream.dreamDate }, today),
        )
    }

    /**
     * Consecutive days with at least one logged dream, counting back from the most recent entry.
     * A streak only counts as current if that entry is from today or yesterday — otherwise it's
     * considered broken and the result is 0.
     */
    fun currentStreak(dreamDates: List<LocalDate>, today: LocalDate = LocalDate.now()): Int {
        if (dreamDates.isEmpty()) return 0
        val distinctDaysDesc = dreamDates.distinct().sortedDescending()
        if (ChronoUnit.DAYS.between(distinctDaysDesc.first(), today) > 1) return 0

        var streak = 1
        var cursor = distinctDaysDesc.first()
        for (i in 1 until distinctDaysDesc.size) {
            val day = distinctDaysDesc[i]
            if (ChronoUnit.DAYS.between(day, cursor) == 1L) {
                streak++
                cursor = day
            } else {
                break
            }
        }
        return streak
    }
}
