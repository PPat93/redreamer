package com.parrotworks.redreamer.ui.list

import com.parrotworks.redreamer.dream
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DreamListFiltersTest {

    private val jul01 = LocalDate.of(2026, 7, 1)
    private val jul10 = LocalDate.of(2026, 7, 10)
    private val jul20 = LocalDate.of(2026, 7, 20)

    private val dreams = listOf(
        dream(id = 1, dreamDate = jul01, isLucid = true, tags = listOf("flying")),
        dream(id = 2, dreamDate = jul10, isNightmare = true, tags = listOf("chase")),
        dream(id = 3, dreamDate = jul20, isRecurring = true, tags = listOf("flying", "water")),
    )

    private fun idsAfter(filters: DreamListFilters) = dreams.filteredBy(filters).map { it.dream.id }

    @Test
    fun `no active filters returns the list untouched`() {
        val filters = DreamListFilters()
        assertEquals(0, filters.activeCount)
        assertEquals(dreams, dreams.filteredBy(filters))
    }

    @Test
    fun `start date bound is inclusive`() {
        assertEquals(listOf(2L, 3L), idsAfter(DreamListFilters(startDate = jul10)))
    }

    @Test
    fun `end date bound is inclusive`() {
        assertEquals(listOf(1L, 2L), idsAfter(DreamListFilters(endDate = jul10)))
    }

    @Test
    fun `date range keeps only dreams inside both bounds`() {
        assertEquals(listOf(2L), idsAfter(DreamListFilters(startDate = jul10, endDate = jul10)))
    }

    @Test
    fun `tag filter matches dreams carrying any selected tag`() {
        assertEquals(listOf(1L, 3L), idsAfter(DreamListFilters(tagNames = setOf("flying"))))
        assertEquals(listOf(1L, 2L, 3L), idsAfter(DreamListFilters(tagNames = setOf("flying", "chase"))))
    }

    @Test
    fun `dream type flags narrow to matching dreams`() {
        assertEquals(listOf(1L), idsAfter(DreamListFilters(lucidOnly = true)))
        assertEquals(listOf(2L), idsAfter(DreamListFilters(nightmareOnly = true)))
        assertEquals(listOf(3L), idsAfter(DreamListFilters(recurringOnly = true)))
    }

    @Test
    fun `filters combine with AND`() {
        // Lucid is only dream 1, which is outside this date range, so nothing matches.
        assertEquals(emptyList<Long>(), idsAfter(DreamListFilters(startDate = jul10, lucidOnly = true)))
    }

    @Test
    fun `activeCount counts a date range once regardless of which bounds are set`() {
        assertEquals(1, DreamListFilters(startDate = jul01).activeCount)
        assertEquals(1, DreamListFilters(endDate = jul01).activeCount)
        assertEquals(1, DreamListFilters(startDate = jul01, endDate = jul20).activeCount)
    }

    @Test
    fun `activeCount sums each distinct active filter`() {
        val filters = DreamListFilters(
            startDate = jul01,
            tagNames = setOf("flying"),
            lucidOnly = true,
            nightmareOnly = true,
            recurringOnly = true,
        )
        assertEquals(5, filters.activeCount)
    }
}
