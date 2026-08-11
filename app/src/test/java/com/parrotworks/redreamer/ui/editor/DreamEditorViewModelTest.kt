package com.parrotworks.redreamer.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.parrotworks.redreamer.MainDispatcherRule
import com.parrotworks.redreamer.data.AppDatabase
import com.parrotworks.redreamer.data.Mood
import com.parrotworks.redreamer.repository.DreamRepository
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The editor is where data loss would hurt most, so these run against a real database and cover
 * the failure modes that actually bit: duplicated dreams, drifting creation time, and drafts
 * evaporating when the process is killed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DreamEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: DreamRepository
    private lateinit var applicationScope: CoroutineScope

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
        applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @After
    fun tearDown() {
        applicationScope.cancel()
        database.close()
    }

    private fun newEditor(savedState: SavedStateHandle = SavedStateHandle()) =
        DreamEditorViewModel(repository, applicationScope, savedState)

    private suspend fun liveDreams() = repository.observeLiveDreams().first()

    /**
     * Simulates the ViewModel being destroyed, which is what leaving the editor does. onCleared is
     * protected, so reach it reflectively rather than widening the production API for a test.
     */
    private fun DreamEditorViewModel.callOnCleared() {
        ViewModel::class.java.getDeclaredMethod("onCleared").apply { isAccessible = true }.invoke(this)
    }

    /** saveNow launches into viewModelScope and returns immediately; wait for the write to land. */
    private suspend fun DreamEditorViewModel.saveAndWait() {
        val done = CompletableDeferred<Unit>()
        saveNow { done.complete(Unit) }
        done.await()
    }

    /**
     * The editor loads an existing dream asynchronously, so the first state is still blank. The
     * wait runs on a real dispatcher: runTest's virtual clock would expire the timeout instantly
     * while the database is answering on its own thread.
     */
    private suspend fun DreamEditorViewModel.awaitState(
        predicate: (DreamEditorUiState) -> Boolean,
    ): DreamEditorUiState = withContext(Dispatchers.Default) {
        withTimeout(5_000) { uiState.first(predicate) }
    }

    @Test
    fun savingWritesTheDreamWithEveryFieldSet() = runTest {
        val editor = newEditor()
        editor.onTitleChange("Flying")
        editor.onContentChange("Above the sea")
        editor.onNotesChange("Felt free")
        editor.onDreamDateChange(LocalDate.of(2026, 7, 20))
        editor.onMoodToggle(Mood.JOYFUL)
        editor.onLucidChange(true)
        editor.onLucidityChange(7)
        editor.onAddTag("flying")

        editor.saveAndWait()

        val saved = liveDreams().single()
        assertEquals("Flying", saved.dream.title)
        assertEquals("Above the sea", saved.dream.content)
        assertEquals("Felt free", saved.dream.notes)
        assertEquals(LocalDate.of(2026, 7, 20), saved.dream.dreamDate)
        assertEquals(setOf(Mood.JOYFUL), saved.dream.moods)
        assertEquals(7, saved.dream.lucidity)
        assertEquals("flying", saved.tags.single().name)
    }

    @Test
    fun anEmptyDreamIsNeverPersisted() = runTest {
        val editor = newEditor()
        editor.onClarityChange(9)
        editor.onMoodToggle(Mood.ANXIOUS)

        editor.saveAndWait()

        assertTrue("mood and clarity alone don't make a dream", liveDreams().isEmpty())
    }

    @Test
    fun repeatedSavesUpdateOneDreamRatherThanCreatingCopies() = runTest {
        val editor = newEditor()
        editor.onTitleChange("Flying")
        editor.saveAndWait()

        editor.onContentChange("Above the sea")
        editor.saveAndWait()
        editor.onContentChange("Above the mountains")
        editor.saveAndWait()

        val dreams = liveDreams()
        assertEquals("still one dream", 1, dreams.size)
        assertEquals("Above the mountains", dreams.single().dream.content)
    }

    @Test
    fun overlappingSavesCannotDuplicateTheDream() = runTest {
        val editor = newEditor()
        editor.onTitleChange("Flying")
        editor.onContentChange("Above the sea")

        // Two saves racing is exactly what a debounced autosave plus a Save tap looked like.
        val first = async { editor.saveAndWait() }
        val second = async { editor.saveAndWait() }
        first.await()
        second.await()

        assertEquals("a race must not insert a second copy", 1, liveDreams().size)
    }

    @Test
    fun creationTimeStaysFixedAcrossSaves() = runTest {
        val editor = newEditor()
        editor.onTitleChange("Flying")
        editor.saveAndWait()
        val firstCreatedAt = liveDreams().single().dream.createdAt

        editor.onContentChange("more text")
        editor.saveAndWait()
        editor.onContentChange("even more text")
        editor.saveAndWait()

        assertEquals(
            "createdAt is the list's tiebreak sort key and must not drift",
            firstCreatedAt,
            liveDreams().single().dream.createdAt,
        )
    }

    @Test
    fun aDraftSurvivesTheProcessBeingKilled() = runTest {
        val savedState = SavedStateHandle()
        val editor = newEditor(savedState)
        editor.onTitleChange("Half written")
        editor.onContentChange("I was somewhere and then")
        editor.onAddTag("flying")
        editor.onTagInputChange("wat")

        // Same SavedStateHandle, brand new ViewModel — what Android does after killing the process.
        val restored = newEditor(savedState)

        restored.uiState.test {
            val state = awaitItem()
            assertEquals("Half written", state.title)
            assertEquals("I was somewhere and then", state.content)
            assertEquals(listOf("flying"), state.tags)
            assertEquals("half-typed tag text is worth keeping too", "wat", state.tagInput)
            assertTrue(state.isReady)
        }
    }

    @Test
    fun aRestoredDraftKeepsEditingTheSameDreamInsteadOfForkingANewOne() = runTest {
        val savedState = SavedStateHandle()
        val editor = newEditor(savedState)
        editor.onTitleChange("Flying")
        editor.onContentChange("Above the sea")
        editor.saveAndWait()

        // saveNow clears the stash, so re-stash by editing again before the simulated kill.
        editor.onContentChange("Above the sea, then higher")
        val restored = newEditor(savedState)
        restored.saveAndWait()

        val dreams = liveDreams()
        assertEquals("the restored draft must update, not duplicate", 1, dreams.size)
        assertEquals("Above the sea, then higher", dreams.single().dream.content)
    }

    @Test
    fun editingAnExistingDreamLoadsItsCurrentValues() = runTest {
        val id = repository.saveDream(
            id = null,
            title = "Flying",
            content = "Above the sea",
            notes = "note",
            dreamDate = LocalDate.of(2026, 7, 20),
            isLucid = true,
            lucidity = 6,
            clarity = 8,
            isNightmare = false,
            isRecurring = true,
            moods = setOf(Mood.PEACEFUL),
            tagNames = listOf("flying"),
        )

        val editor = newEditor(SavedStateHandle(mapOf("dreamId" to id)))

        val state = editor.awaitState { it.isReady && it.title.isNotEmpty() }
        assertTrue(state.isEditingExisting)
        assertEquals("Flying", state.title)
        assertEquals("Above the sea", state.content)
        assertEquals("note", state.notes)
        assertEquals(6, state.lucidity)
        assertEquals(8, state.clarity)
        assertTrue(state.isRecurring)
        assertEquals(setOf(Mood.PEACEFUL), state.moods)
        assertEquals(listOf("flying"), state.tags)
    }

    @Test
    fun leavingTheEditorBeforeTheAutosaveFiresStillKeepsTheText() = runTest {
        val editor = newEditor()
        editor.onTitleChange("Half written")
        editor.onContentChange("I was flying and then")

        // Pressing back destroys the ViewModel well inside the debounce window; the pending
        // autosave dies with it, so the flush has to happen here or the text is gone for good.
        editor.callOnCleared()

        val saved = withContext(Dispatchers.Default) {
            withTimeout(5_000) { repository.observeLiveDreams().first { it.isNotEmpty() } }
        }
        assertEquals("I was flying and then", saved.single().dream.content)
    }

    @Test
    fun leavingAnUntouchedEditorWritesNothing() = runTest {
        val editor = newEditor()

        editor.callOnCleared()

        assertTrue("opening and closing the editor must not create a dream", liveDreams().isEmpty())
    }

    @Test
    fun emptyingAnAutosavedDraftDiscardsItInsteadOfKeepingDeletedText() = runTest {
        val editor = newEditor()
        editor.onTitleChange("Flying")
        editor.onContentChange("Above the sea")
        editor.saveAndWait()
        assertEquals(1, liveDreams().size)

        // Changed their mind and cleared the form.
        editor.onTitleChange("")
        editor.onContentChange("")
        editor.saveAndWait()

        assertTrue("an emptied draft must not survive holding the deleted text", liveDreams().isEmpty())
    }

    @Test
    fun clearingAnExistingDreamDoesNotDeleteIt() = runTest {
        val id = repository.saveDream(
            id = null,
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
            tagNames = emptyList(),
        )
        val editor = newEditor(SavedStateHandle(mapOf("dreamId" to id)))
        editor.awaitState { it.isReady && it.title.isNotEmpty() }

        editor.onTitleChange("")
        editor.onContentChange("")
        editor.saveAndWait()

        assertEquals(
            "emptying a stored dream's fields must never delete the dream itself",
            1,
            liveDreams().size,
        )
    }

    @Test
    fun duplicateTagsAreIgnoredRegardlessOfCase() = runTest {
        val editor = newEditor()
        editor.onTitleChange("Flying")
        editor.onAddTag("flying")
        editor.onAddTag("Flying")
        editor.onAddTag("  flying  ")

        assertEquals(listOf("flying"), editor.uiState.value.tags)
    }
}
