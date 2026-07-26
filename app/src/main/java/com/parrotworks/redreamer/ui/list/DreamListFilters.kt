package com.parrotworks.redreamer.ui.list

import com.parrotworks.redreamer.data.DreamWithTags
import java.time.LocalDate

data class DreamListFilters(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val tagNames: Set<String> = emptySet(),
    val lucidOnly: Boolean = false,
    val nightmareOnly: Boolean = false,
    val recurringOnly: Boolean = false,
) {
    /** How many distinct filters are narrowing the list — drives the badge on the filter button. */
    val activeCount: Int
        get() = listOf(
            startDate != null || endDate != null,
            tagNames.isNotEmpty(),
            lucidOnly,
            nightmareOnly,
            recurringOnly,
        ).count { it }
}

/**
 * Filters are combined with AND: every active one must match. Date bounds are inclusive, and a
 * dream matches the tag filter if it carries *any* of the selected tags.
 */
fun List<DreamWithTags>.filteredBy(filters: DreamListFilters): List<DreamWithTags> {
    if (filters.activeCount == 0) return this
    return filter { dreamWithTags ->
        val dream = dreamWithTags.dream
        (filters.startDate == null || !dream.dreamDate.isBefore(filters.startDate)) &&
            (filters.endDate == null || !dream.dreamDate.isAfter(filters.endDate)) &&
            (filters.tagNames.isEmpty() || dreamWithTags.tags.any { it.name in filters.tagNames }) &&
            (!filters.lucidOnly || dream.isLucid) &&
            (!filters.nightmareOnly || dream.isNightmare) &&
            (!filters.recurringOnly || dream.isRecurring)
    }
}
