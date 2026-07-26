package com.parrotworks.redreamer.repository

import com.parrotworks.redreamer.data.Mood
import com.parrotworks.redreamer.dream
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DreamStatsCalculatorTest {

    private val today = LocalDate.of(2026, 7, 24)

    @Test
    fun `empty input returns empty stats`() {
        assertEquals(DreamStats.EMPTY, DreamStatsCalculator.calculate(emptyList(), today))
    }

    @Test
    fun `percentages are share of total dreams`() {
        val stats = DreamStatsCalculator.calculate(
            listOf(
                dream(id = 1, isLucid = true, isNightmare = true),
                dream(id = 2, isLucid = true),
                dream(id = 3, isRecurring = true),
                dream(id = 4),
            ),
            today,
        )

        assertEquals(4, stats.totalDreams)
        assertEquals(50, stats.lucidPercent)
        assertEquals(25, stats.nightmarePercent)
        assertEquals(25, stats.recurringPercent)
    }

    @Test
    fun `average clarity is the mean across dreams`() {
        val stats = DreamStatsCalculator.calculate(
            listOf(dream(id = 1, clarity = 2), dream(id = 2, clarity = 5), dream(id = 3, clarity = 8)),
            today,
        )
        assertEquals(5.0, stats.averageClarity, 0.001)
    }

    @Test
    fun `mood counts tally every occurrence across dreams`() {
        val stats = DreamStatsCalculator.calculate(
            listOf(
                dream(id = 1, moods = setOf(Mood.JOYFUL, Mood.ANXIOUS)),
                dream(id = 2, moods = setOf(Mood.JOYFUL)),
            ),
            today,
        )

        assertEquals(2, stats.moodCounts[Mood.JOYFUL])
        assertEquals(1, stats.moodCounts[Mood.ANXIOUS])
        assertEquals(null, stats.moodCounts[Mood.SAD])
    }

    @Test
    fun `top tags are ordered by usage and capped`() {
        val dreams = listOf(
            dream(id = 1, tags = listOf("flying", "water")),
            dream(id = 2, tags = listOf("flying")),
            dream(id = 3, tags = listOf("flying", "water")),
            dream(id = 4, tags = listOf("chase")),
        )

        val topTags = DreamStatsCalculator.calculate(dreams, today).topTags

        assertEquals(TagCount("flying", 3), topTags[0])
        assertEquals(TagCount("water", 2), topTags[1])
        assertEquals(TagCount("chase", 1), topTags[2])
        assertTrue(topTags.size <= DreamStatsCalculator.TOP_TAGS_LIMIT)
    }

    @Test
    fun `dreams per month covers a fixed window ending this month`() {
        val stats = DreamStatsCalculator.calculate(
            listOf(
                dream(id = 1, dreamDate = LocalDate.of(2026, 7, 3)),
                dream(id = 2, dreamDate = LocalDate.of(2026, 7, 20)),
                dream(id = 3, dreamDate = LocalDate.of(2026, 6, 15)),
                // Outside the window entirely, so it must not appear.
                dream(id = 4, dreamDate = LocalDate.of(2025, 1, 1)),
            ),
            today,
        )

        assertEquals(DreamStatsCalculator.MONTHS_BACK, stats.dreamsPerMonth.size)
        assertEquals(YearMonth.of(2026, 7), stats.dreamsPerMonth.last().yearMonth)
        assertEquals(2, stats.dreamsPerMonth.last().count)
        assertEquals(1, stats.dreamsPerMonth.first { it.yearMonth == YearMonth.of(2026, 6) }.count)
        assertEquals(3, stats.dreamsPerMonth.sumOf { it.count })
    }

    @Test
    fun `streak is zero when there are no dreams`() {
        assertEquals(0, DreamStatsCalculator.currentStreak(emptyList(), today))
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        val dates = listOf(today, today.minusDays(1), today.minusDays(2))
        assertEquals(3, DreamStatsCalculator.currentStreak(dates, today))
    }

    @Test
    fun `streak still counts when the latest entry is yesterday`() {
        val dates = listOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, DreamStatsCalculator.currentStreak(dates, today))
    }

    @Test
    fun `streak is broken when the latest entry is older than yesterday`() {
        val dates = listOf(today.minusDays(2), today.minusDays(3))
        assertEquals(0, DreamStatsCalculator.currentStreak(dates, today))
    }

    @Test
    fun `streak stops at the first gap`() {
        val dates = listOf(today, today.minusDays(1), today.minusDays(3), today.minusDays(4))
        assertEquals(2, DreamStatsCalculator.currentStreak(dates, today))
    }

    @Test
    fun `multiple dreams on one day count as a single streak day`() {
        val dates = listOf(today, today, today.minusDays(1))
        assertEquals(2, DreamStatsCalculator.currentStreak(dates, today))
    }
}
