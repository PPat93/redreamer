package com.parrotworks.redreamer.data.backup

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The export format is the app's only durable escape hatch, so the exact date encoding matters:
 * a change here would silently make older backups unreadable.
 */
class BackupRoundTripTest {

    private val serializer = BackupSerializer()

    @Test
    fun `dream dates are encoded as plain ISO local dates`() {
        val encoded = serializer.encode(
            BackupFile(
                exportedAt = Instant.parse("2026-07-24T09:00:00Z").toString(),
                dreams = listOf(backupDreamOn(LocalDate.of(2026, 7, 20))),
            ),
        )

        assert(encoded.contains("\"dreamDate\": \"2026-07-20\"")) {
            "dreamDate should serialize as an ISO local date, got:\n$encoded"
        }
    }

    @Test
    fun `dates survive a round trip and reparse to the same values`() {
        val original = backupDreamOn(LocalDate.of(2024, 2, 29))
        val restored = serializer
            .decode(serializer.encode(BackupFile(exportedAt = "2026-07-24T09:00:00Z", dreams = listOf(original))))
            .dreams
            .single()

        assertEquals(original, restored)
        assertEquals(LocalDate.parse(original.dreamDate), LocalDate.parse(restored.dreamDate))
        assertEquals(Instant.parse(original.createdAt), Instant.parse(restored.createdAt))
    }

    private fun backupDreamOn(date: LocalDate) = BackupDream(
        title = "Dream",
        content = "Content",
        dreamDate = date.toString(),
        createdAt = Instant.parse("2026-07-20T06:30:00Z").toString(),
        updatedAt = Instant.parse("2026-07-20T06:30:00Z").toString(),
    )
}
