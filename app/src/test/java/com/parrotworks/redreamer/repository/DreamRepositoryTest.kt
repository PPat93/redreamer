package com.parrotworks.redreamer.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.parrotworks.redreamer.data.AppDatabase
import com.parrotworks.redreamer.data.Mood
import com.parrotworks.redreamer.data.backup.BackupDream
import com.parrotworks.redreamer.data.backup.BackupFile
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the repository against a real (in-memory) Room database, so the invariants that span
 * several tables — search index staying in step with dreams, tag links, transactional saves — are
 * actually verified rather than assumed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DreamRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: DreamRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

        repository = DreamRepository(
            database = database,
            dreamDao = database.dreamDao(),
            tagDao = database.tagDao(),
            dreamFtsDao = database.dreamFtsDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun addDream(
        title: String = "Flying",
        content: String = "Above the sea",
        notes: String = "",
        dreamDate: LocalDate = LocalDate.of(2026, 7, 20),
        moods: Set<Mood> = emptySet(),
        tags: List<String> = emptyList(),
        createdAt: Instant? = null,
    ): Long = repository.saveDream(
        id = null,
        title = title,
        content = content,
        notes = notes,
        dreamDate = dreamDate,
        isLucid = false,
        lucidity = null,
        clarity = 5,
        isNightmare = false,
        isRecurring = false,
        moods = moods,
        tagNames = tags,
        existingCreatedAt = createdAt,
    )

    @Test
    fun savedDreamIsImmediatelySearchable() = runTest {
        addDream(title = "Flying", content = "Above the sea")

        val hits = repository.searchDreams("sea").first()

        assertEquals(1, hits.size)
        assertEquals("Flying", hits.first().dream.title)
    }

    @Test
    fun editingADreamUpdatesTheSearchIndex() = runTest {
        val id = addDream(title = "Flying", content = "Above the sea")

        repository.saveDream(
            id = id,
            title = "Flying",
            content = "Above the mountains",
            notes = "",
            dreamDate = LocalDate.of(2026, 7, 20),
            isLucid = false,
            lucidity = null,
            clarity = 5,
            isNightmare = false,
            isRecurring = false,
            moods = emptySet(),
            tagNames = emptyList(),
            existingCreatedAt = Instant.parse("2026-07-20T06:00:00Z"),
        )

        assertTrue("stale text must stop matching", repository.searchDreams("sea").first().isEmpty())
        assertEquals(1, repository.searchDreams("mountains").first().size)
    }

    @Test
    fun editingADreamDoesNotMoveItInTheList() = runTest {
        // The whole point of dreamDate + createdAt as sort keys: revising an old entry must not
        // bump it to the top the way an updatedAt sort would.
        addDream(title = "Oldest", dreamDate = LocalDate.of(2026, 7, 1))
        val middleId = addDream(title = "Middle", dreamDate = LocalDate.of(2026, 7, 10))
        addDream(title = "Newest", dreamDate = LocalDate.of(2026, 7, 20))
        val originalCreatedAt = repository.observeDream(middleId).first()!!.dream.createdAt

        repository.saveDream(
            id = middleId,
            title = "Middle, revised",
            content = "new text entirely",
            notes = "",
            dreamDate = LocalDate.of(2026, 7, 10),
            isLucid = false,
            lucidity = null,
            clarity = 5,
            isNightmare = false,
            isRecurring = false,
            moods = emptySet(),
            tagNames = emptyList(),
            existingCreatedAt = originalCreatedAt,
        )

        assertEquals(
            listOf("Newest", "Middle, revised", "Oldest"),
            repository.observeLiveDreams().first().map { it.dream.title },
        )
    }

    @Test
    fun editingRepairsAMissingSearchIndexRow() = runTest {
        val id = addDream(content = "Above the sea")
        // Simulate index drift, however it might arise.
        database.dreamFtsDao().deleteByDreamId(id)
        assertTrue(repository.searchDreams("sea").first().isEmpty())

        repository.saveDream(
            id = id,
            title = "Flying",
            content = "Above the mountains",
            notes = "",
            dreamDate = LocalDate.of(2026, 7, 20),
            isLucid = false,
            lucidity = null,
            clarity = 5,
            isNightmare = false,
            isRecurring = false,
            moods = emptySet(),
            tagNames = emptyList(),
            existingCreatedAt = Instant.parse("2026-07-20T06:00:00Z"),
        )

        assertEquals(
            "an edit should restore searchability rather than silently no-op",
            1,
            repository.searchDreams("mountains").first().size,
        )
    }

    @Test
    fun binnedDreamsLeaveTheListAndTheSearchIndex() = runTest {
        val id = addDream(content = "Above the sea")

        repository.softDelete(id)

        assertTrue(repository.observeLiveDreams().first().isEmpty())
        assertTrue("binned dreams must not be searchable", repository.searchDreams("sea").first().isEmpty())
        assertEquals(1, repository.observeBinnedDreams().first().size)
        assertEquals(1, repository.observeBinnedCount().first())
    }

    @Test
    fun restoringADreamMakesItSearchableAgain() = runTest {
        val id = addDream(content = "Above the sea")
        repository.softDelete(id)

        repository.restore(id)

        assertEquals(1, repository.observeLiveDreams().first().size)
        assertEquals("restore must rebuild the index row", 1, repository.searchDreams("sea").first().size)
        assertTrue(repository.observeBinnedDreams().first().isEmpty())
    }

    @Test
    fun restoringTwiceDoesNotCollideOnTheIndexRow() = runTest {
        val id = addDream(content = "Above the sea")
        repository.softDelete(id)

        repository.restore(id)
        repository.restore(id)

        assertEquals("a second restore must not duplicate or crash", 1, repository.searchDreams("sea").first().size)
    }

    @Test
    fun massDeleteBinsEverySelectedDream() = runTest {
        val first = addDream(title = "One", content = "alpha")
        val second = addDream(title = "Two", content = "beta")
        addDream(title = "Three", content = "gamma")

        repository.softDeleteAll(listOf(first, second))

        assertEquals(1, repository.observeLiveDreams().first().size)
        assertEquals(2, repository.observeBinnedCount().first())
        assertTrue(repository.searchDreams("alpha").first().isEmpty())
    }

    @Test
    fun savingKeepsTagLinksInStepWithTheDream() = runTest {
        val id = addDream(tags = listOf("flying", "water"))

        assertEquals(2, repository.observeLiveDreams().first().single().tags.size)

        repository.saveDream(
            id = id,
            title = "Flying",
            content = "Above the sea",
            notes = "",
            dreamDate = LocalDate.of(2026, 7, 20),
            isLucid = false,
            lucidity = null,
            clarity = 5,
            isNightmare = false,
            isRecurring = false,
            moods = emptySet(),
            tagNames = listOf("flying"),
            existingCreatedAt = Instant.parse("2026-07-20T06:00:00Z"),
        )

        val tags = repository.observeLiveDreams().first().single().tags
        assertEquals("removed tags must be unlinked", 1, tags.size)
        assertEquals("flying", tags.single().name)
    }

    @Test
    fun tagsAreReusedCaseInsensitivelyRatherThanDuplicated() = runTest {
        addDream(title = "One", tags = listOf("Flying"))
        addDream(title = "Two", tags = listOf("flying"))

        assertEquals("one tag, not two spellings", 1, repository.observeAllTags().first().size)
    }

    @Test
    fun renamingOntoAnExistingNameMergesTheTwoTags() = runTest {
        addDream(title = "One", tags = listOf("flying"))
        addDream(title = "Two", tags = listOf("flight"))
        val flight = repository.observeAllTags().first().single { it.name == "flight" }

        repository.renameOrMergeTag(flight.id, "flying")

        val tags = repository.observeAllTags().first()
        assertEquals("the duplicate must be absorbed", 1, tags.size)
        assertEquals("flying", tags.single().name)

        val usage = repository.observeTagsWithUsage().first().single()
        assertEquals("both dreams now carry the surviving tag", 2, usage.usageCount)
    }

    @Test
    fun renamingToADifferentCaseKeepsTheSameTag() = runTest {
        addDream(tags = listOf("flying"))
        val tag = repository.observeAllTags().first().single()

        repository.renameOrMergeTag(tag.id, "Flying")

        val tags = repository.observeAllTags().first()
        assertEquals(1, tags.size)
        assertEquals("Flying", tags.single().name)
    }

    @Test
    fun deletingATagUnlinksItFromDreams() = runTest {
        addDream(tags = listOf("flying"))
        val tag = repository.observeAllTags().first().single()

        repository.deleteTag(tag.id)

        assertTrue(repository.observeAllTags().first().isEmpty())
        assertTrue(
            "the dream survives, only the link goes",
            repository.observeLiveDreams().first().single().tags.isEmpty(),
        )
    }

    @Test
    fun exportSkipsBinnedDreamsButKeepsUnusedTags() = runTest {
        addDream(title = "Kept", tags = listOf("flying"))
        val binned = addDream(title = "Binned")
        repository.softDelete(binned)
        repository.createTag("never-used")

        val snapshot = repository.exportSnapshot()

        assertEquals(1, snapshot.dreams.size)
        assertEquals("Kept", snapshot.dreams.single().title)
        assertTrue("standalone tags must survive a round trip", "never-used" in snapshot.tags)
    }

    @Test
    fun importAddsDreamsAndReportsUnreadableEntries() = runTest {
        val backup = BackupFile(
            exportedAt = Instant.now().toString(),
            tags = listOf("flying"),
            dreams = listOf(
                BackupDream(
                    title = "Good",
                    content = "Above the sea",
                    dreamDate = "2026-07-20",
                    createdAt = "2026-07-20T06:00:00Z",
                    updatedAt = "2026-07-20T06:00:00Z",
                    moods = listOf("JOYFUL"),
                    tags = listOf("flying"),
                ),
                BackupDream(
                    title = "Broken date",
                    content = "unreadable",
                    dreamDate = "not-a-date",
                    createdAt = "2026-07-20T06:00:00Z",
                    updatedAt = "2026-07-20T06:00:00Z",
                ),
            ),
        )

        val result = repository.importBackup(backup)

        assertEquals(1, result.imported)
        assertEquals("a dropped entry must be reported, not silently lost", 1, result.skipped)

        val imported = repository.observeLiveDreams().first().single()
        assertEquals("Good", imported.dream.title)
        assertEquals(setOf(Mood.JOYFUL), imported.dream.moods)
        assertEquals("flying", imported.tags.single().name)
        assertEquals("original creation time is preserved", Instant.parse("2026-07-20T06:00:00Z"), imported.dream.createdAt)
        assertEquals("imported dreams are searchable", 1, repository.searchDreams("sea").first().size)
    }

    @Test
    fun importedDreamsKeepTheirOriginalDreamDateOrdering() = runTest {
        addDream(title = "Recent", dreamDate = LocalDate.of(2026, 7, 25))
        val backup = BackupFile(
            exportedAt = Instant.now().toString(),
            dreams = listOf(
                BackupDream(
                    title = "Older",
                    content = "from the archive",
                    dreamDate = "2020-01-01",
                    createdAt = "2020-01-01T06:00:00Z",
                    updatedAt = "2020-01-01T06:00:00Z",
                ),
            ),
        )

        repository.importBackup(backup)

        val titles = repository.observeLiveDreams().first().map { it.dream.title }
        assertEquals("an old imported dream must not jump to the top", listOf("Recent", "Older"), titles)
    }

    @Test
    fun permanentDeleteRemovesTheDreamEntirely() = runTest {
        val id = addDream()
        repository.softDelete(id)

        repository.deleteForever(id)

        assertTrue(repository.observeBinnedDreams().first().isEmpty())
        assertNull(repository.observeDream(id).first())
    }

    @Test
    fun emptyingTheBinLeavesLiveDreamsAlone() = runTest {
        val kept = addDream(title = "Kept")
        val binned = addDream(title = "Binned")
        repository.softDelete(binned)

        repository.emptyBin()

        assertEquals(0, repository.observeBinnedCount().first())
        assertNotNull(repository.observeDream(kept).first())
    }
}
