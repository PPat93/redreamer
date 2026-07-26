package com.parrotworks.redreamer.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializerTest {

    private val serializer = BackupSerializer()

    private val sample = BackupFile(
        exportedAt = "2026-07-24T09:00:00Z",
        tags = listOf("flying", "unused-tag"),
        dreams = listOf(
            BackupDream(
                title = "Flying over the sea",
                content = "I was above the water",
                notes = "Felt free",
                dreamDate = "2026-07-20",
                createdAt = "2026-07-20T06:30:00Z",
                updatedAt = "2026-07-20T06:45:00Z",
                isLucid = true,
                lucidity = 7,
                clarity = 8,
                isNightmare = false,
                isRecurring = true,
                moods = listOf("JOYFUL", "PEACEFUL"),
                tags = listOf("flying"),
            ),
        ),
    )

    @Test
    fun `round trip preserves every field`() {
        assertEquals(sample, serializer.decode(serializer.encode(sample)))
    }

    @Test
    fun `standalone tags survive the round trip`() {
        val restored = serializer.decode(serializer.encode(sample))
        assertTrue("unused-tag" in restored.tags)
    }

    @Test
    fun `null lucidity survives as null rather than becoming zero`() {
        val nonLucid = sample.copy(
            dreams = listOf(sample.dreams.first().copy(isLucid = false, lucidity = null)),
        )
        assertEquals(null, serializer.decode(serializer.encode(nonLucid)).dreams.first().lucidity)
    }

    @Test
    fun `unknown fields from a newer format are ignored`() {
        val raw = """
            {
              "version": 1,
              "exportedAt": "2026-07-24T09:00:00Z",
              "somethingFromTheFuture": true,
              "tags": ["flying"],
              "dreams": []
            }
        """.trimIndent()

        val decoded = serializer.decode(raw)
        assertEquals(listOf("flying"), decoded.tags)
        assertEquals(emptyList<BackupDream>(), decoded.dreams)
    }

    @Test
    fun `omitted optional fields fall back to defaults`() {
        val raw = """
            {
              "exportedAt": "2026-07-24T09:00:00Z",
              "dreams": [
                {
                  "dreamDate": "2026-07-20",
                  "createdAt": "2026-07-20T06:30:00Z",
                  "updatedAt": "2026-07-20T06:30:00Z"
                }
              ]
            }
        """.trimIndent()

        val decoded = serializer.decode(raw)
        val dreamEntry = decoded.dreams.single()

        assertEquals(BackupFile.CURRENT_VERSION, decoded.version)
        assertEquals(emptyList<String>(), decoded.tags)
        assertEquals("", dreamEntry.title)
        assertEquals("", dreamEntry.notes)
        assertEquals(5, dreamEntry.clarity)
        assertEquals(false, dreamEntry.isLucid)
        assertEquals(null, dreamEntry.lucidity)
        assertEquals(emptyList<String>(), dreamEntry.moods)
        assertEquals(emptyList<String>(), dreamEntry.tags)
    }
}
