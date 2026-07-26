package com.parrotworks.redreamer.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtsQueryBuilderTest {

    @Test
    fun `single term becomes a quoted prefix match`() {
        assertEquals("\"fly\"*", FtsQueryBuilder.build("fly"))
    }

    @Test
    fun `multiple terms are ANDed together`() {
        assertEquals("\"fly\"* \"house\"*", FtsQueryBuilder.build("fly house"))
    }

    @Test
    fun `surrounding and repeated whitespace is ignored`() {
        assertEquals("\"fly\"* \"house\"*", FtsQueryBuilder.build("   fly    house  "))
    }

    @Test
    fun `blank input yields null so callers can skip the query`() {
        assertNull(FtsQueryBuilder.build(""))
        assertNull(FtsQueryBuilder.build("    "))
    }

    @Test
    fun `quotes are stripped rather than passed through to MATCH`() {
        assertEquals("\"fly\"*", FtsQueryBuilder.build("\"fly\""))
    }

    @Test
    fun `input consisting only of syntax characters yields null`() {
        assertNull(FtsQueryBuilder.build("\"\""))
        assertNull(FtsQueryBuilder.build("*"))
    }

    @Test
    fun `embedded wildcards are stripped so the prefix match stays well formed`() {
        assertEquals("\"fly\"*", FtsQueryBuilder.build("fl*y*"))
    }
}
