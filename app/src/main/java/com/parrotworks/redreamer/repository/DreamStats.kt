package com.parrotworks.redreamer.repository

import com.parrotworks.redreamer.data.Mood
import java.time.YearMonth

data class TagCount(val name: String, val count: Int)

data class MonthCount(val yearMonth: YearMonth, val count: Int)

data class DreamStats(
    val totalDreams: Int,
    val lucidPercent: Int,
    val nightmarePercent: Int,
    val recurringPercent: Int,
    val averageClarity: Double,
    val moodCounts: Map<Mood, Int>,
    val topTags: List<TagCount>,
    val dreamsPerMonth: List<MonthCount>,
    val currentStreakDays: Int,
) {
    companion object {
        val EMPTY = DreamStats(
            totalDreams = 0,
            lucidPercent = 0,
            nightmarePercent = 0,
            recurringPercent = 0,
            averageClarity = 0.0,
            moodCounts = emptyMap(),
            topTags = emptyList(),
            dreamsPerMonth = emptyList(),
            currentStreakDays = 0,
        )
    }
}
