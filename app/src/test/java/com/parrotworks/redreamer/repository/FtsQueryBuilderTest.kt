package com.parrotworks.redreamer.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Note these assert the *shape* of the query string. That isn't enough on its own — an earlier
 * version produced `"fore"*`, which looks like a prefix match and passed tests like these, but is
 * FTS5 syntax that FTS4 silently treats as an exact phrase. DreamListViewModelTest exercises the
 * real database and is what actually proves prefix matching works.
 */
class FtsQueryBuilderTest {

    @Test
    fun `single term becomes a prefix match`() {
        assertEquals("fly*", FtsQueryBuilder.build("fly"))
    }

    @Test
    fun `multiple terms are ANDed together`() {
        assertEquals("fly* house*", FtsQueryBuilder.build("fly house"))
    }

    @Test
    fun `surrounding and repeated whitespace is ignored`() {
        assertEquals("fly* house*", FtsQueryBuilder.build("   fly    house  "))
    }

    @Test
    fun `blank input yields null so callers can skip the query`() {
        assertNull(FtsQueryBuilder.build(""))
        assertNull(FtsQueryBuilder.build("    "))
    }

    @Test
    fun `MATCH operator characters are stripped rather than passed through`() {
        // Each of these would otherwise be interpreted as syntax and could fail the query.
        assertEquals("fly*", FtsQueryBuilder.build("\"fly\""))
        assertEquals("fly*", FtsQueryBuilder.build("-fly"))
        assertEquals("fly*", FtsQueryBuilder.build("^fly"))
        assertEquals("fly*", FtsQueryBuilder.build("fl*y*"))
    }

    @Test
    fun `input consisting only of syntax characters yields null`() {
        assertNull(FtsQueryBuilder.build("\"\""))
        assertNull(FtsQueryBuilder.build("*"))
        assertNull(FtsQueryBuilder.build("-^()"))
    }

    @Test
    fun `letters outside the latin alphabet survive`() {
        // Dreams get written in whatever language the dreamer thinks in.
        assertEquals("łąka*", FtsQueryBuilder.build("łąka"))
        assertEquals("mgła* nocą*", FtsQueryBuilder.build("mgła nocą"))
    }

    @Test
    fun `digits and underscores are kept`() {
        assertEquals("room_101*", FtsQueryBuilder.build("room_101"))
    }
}
