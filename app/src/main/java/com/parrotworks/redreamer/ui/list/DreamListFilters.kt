package com.parrotworks.redreamer.ui.list

import java.time.LocalDate

data class DreamListFilters(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val tagNames: Set<String> = emptySet(),
    val lucidOnly: Boolean = false,
    val nightmareOnly: Boolean = false,
    val recurringOnly: Boolean = false,
) {
    val activeCount: Int
        get() = listOf(
            startDate != null || endDate != null,
            tagNames.isNotEmpty(),
            lucidOnly,
            nightmareOnly,
            recurringOnly,
        ).count { it }
}
