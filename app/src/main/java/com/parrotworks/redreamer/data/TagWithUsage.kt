package com.parrotworks.redreamer.data

/** Query-result projection — not a stored entity, just [Tag] plus how many dreams reference it. */
data class TagWithUsage(
    val id: Long,
    val name: String,
    val color: Int?,
    val usageCount: Int,
)
