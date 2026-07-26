package com.parrotworks.redreamer.repository

/** Turns raw user input into a safe SQLite FTS4 MATCH expression. */
object FtsQueryBuilder {

    /**
     * Each whitespace-separated term becomes a quoted prefix match, and terms are ANDed, so
     * `fly hous` matches a dream containing both "flying" and "house". Quotes are stripped rather
     * than escaped: they carry syntactic meaning in MATCH, and passing them through would let a
     * stray `"` produce an invalid query at runtime.
     *
     * @return null when there is nothing searchable, so callers can skip querying entirely.
     */
    fun build(rawQuery: String): String? {
        val terms = rawQuery.trim()
            .split(Regex("\\s+"))
            .map { it.replace("\"", "").replace("*", "").trim() }
            .filter { it.isNotEmpty() }
        if (terms.isEmpty()) return null
        return terms.joinToString(" ") { "\"$it\"*" }
    }
}
