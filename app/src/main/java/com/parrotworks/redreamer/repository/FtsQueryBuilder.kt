package com.parrotworks.redreamer.repository

/** Turns raw user input into a safe SQLite FTS4 MATCH expression. */
object FtsQueryBuilder {

    /**
     * Each whitespace-separated term becomes a prefix match and terms are ANDed, so `fly hous`
     * matches a dream containing both "flying" and "house".
     *
     * Terms are emitted bare followed by `*` because that is FTS4's prefix syntax. Quoting them
     * (`"fly"*`) is FTS5 syntax — in FTS4 the quotes make it an exact phrase and the `*` is
     * ignored, which silently turned every search into whole-word-only matching.
     *
     * Everything that isn't a letter, digit or underscore is stripped instead of escaped. Those
     * characters are MATCH operators (`-` is NOT, `^` anchors, quotes and parens group), so a
     * stray one would otherwise produce a syntax error mid-typing. Unicode letters survive, so
     * accented and non-Latin words still search correctly.
     *
     * @return null when there is nothing searchable, so callers can skip querying entirely.
     */
    fun build(rawQuery: String): String? {
        val terms = rawQuery.trim()
            .split(Regex("\\s+"))
            .map { term -> term.replace(Regex("[^\\p{L}\\p{N}_]"), "") }
            .filter { it.isNotEmpty() }
        if (terms.isEmpty()) return null
        return terms.joinToString(" ") { "$it*" }
    }
}
