package com.parrotworks.redreamer.ui.list

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.parrotworks.redreamer.MainDispatcherRule
import com.parrotworks.redreamer.data.AppDatabase
import com.parrotworks.redreamer.repository.DreamRepository
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers how search, filters and selection combine — the list is the only place a dream can be
 * hidden from the user, so "empty because filtered" must stay distinguishable from "empty because
 * there is nothing".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DreamListViewModelTest {

    // Real dispatcher: the search debounce uses delay while the database answers on its own thread.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(Dispatchers.Default)

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

    private suspend fun seed() {
        repository.saveDream(
            id = null, title = "Flying", content = "Above the sea", notes = "",
            dreamDate = LocalDate.of(2026, 7, 1), isLucid = true, lucidity = 7, clarity = 5,
            isNightmare = false, isRecurring = false, moods = emptySet(), tagNames = listOf("flying"),
        )
        repository.saveDream(
            id = null, title = "Chased", content = "Through a forest", notes = "",
            dreamDate = LocalDate.of(2026, 7, 10), isLucid = false, lucidity = null, clarity = 5,
            isNightmare = true, isRecurring = false, moods = emptySet(), tagNames = listOf("chase"),
        )
        repository.saveDream(
            id = null, title = "Old house", content = "Familiar rooms", notes = "",
            dreamDate = LocalDate.of(2026, 7, 20), isLucid = false, lucidity = null, clarity = 5,
            isNightmare = false, isRecurring = true, moods = emptySet(), tagNames = listOf("flying"),
        )
    }

    private fun newViewModel() = DreamListViewModel(repository)

    /** Real time: the ViewModel mixes a debounce with database reads on background threads. */
    private suspend fun DreamListViewModel.awaitUi(
        predicate: (DreamListUiState) -> Boolean,
    ): DreamListUiState = withContext(Dispatchers.Default) {
        withTimeout(10_000) { uiState.first(predicate) }
    }

    private suspend fun DreamListViewModel.awaitTitles(vararg expected: String): DreamListUiState =
        awaitUi { state -> state.isLoaded && state.dreams.map { it.dream.title } == expected.toList() }

    @Test
    fun listStartsUnloadedSoNoEmptyStateFlashes() = runTest {
        val viewModel = newViewModel()

        assertFalse("nothing is known before the first emission", viewModel.uiState.value.isLoaded)
    }

    @Test
    fun dreamsArriveNewestDreamDateFirst() = runTest {
        seed()
        val viewModel = newViewModel()

        val state = viewModel.awaitUi { it.isLoaded && it.dreams.size == 3 }

        assertEquals(
            listOf("Old house", "Chased", "Flying"),
            state.dreams.map { it.dream.title },
        )
        assertTrue(state.hasAnyDreams)
    }

    @Test
    fun lucidFilterKeepsOnlyLucidDreams() = runTest {
        seed()
        val viewModel = newViewModel()
        viewModel.awaitUi { it.isLoaded && it.dreams.size == 3 }

        viewModel.setLucidOnly(true)

        viewModel.awaitTitles("Flying")
    }

    @Test
    fun nightmareAndRecurringFiltersSelectTheirOwnDreams() = runTest {
        seed()
        val viewModel = newViewModel()
        viewModel.awaitUi { it.isLoaded && it.dreams.size == 3 }

        viewModel.setNightmareOnly(true)
        viewModel.awaitTitles("Chased")

        viewModel.clearFilters()
        viewModel.setRecurringOnly(true)
        viewModel.awaitTitles("Old house")
    }

    @Test
    fun tagFilterMatchesEveryDreamCarryingThatTag() = runTest {
        seed()
        val viewModel = newViewModel()
        viewModel.awaitUi { it.isLoaded && it.dreams.size == 3 }

        viewModel.toggleTagFilter("flying")

        viewModel.awaitTitles("Old house", "Flying")
    }

    @Test
    fun dateRangeBoundsAreInclusive() = runTest {
        seed()
        val viewModel = newViewModel()
        viewModel.awaitUi { it.isLoaded && it.dreams.size == 3 }

        viewModel.setStartDate(LocalDate.of(2026, 7, 10))
        viewModel.setEndDate(LocalDate.of(2026, 7, 20))

        viewModel.awaitTitles("Old house", "Chased")
    }

    @Test
    fun filteringToNothingStillReportsThatDreamsExist() = runTest {
        seed()
        val viewModel = newViewModel()
        viewModel.awaitUi { it.isLoaded && it.dreams.size == 3 }

        // Lucid is only "Flying", which sits outside this date range.
        viewModel.setLucidOnly(true)
        viewModel.setStartDate(LocalDate.of(2026, 7, 10))

        val state = viewModel.awaitUi { it.isLoaded && it.dreams.isEmpty() }
        assertTrue(
            "the screen must say 'no matches', not 'no dreams yet'",
            state.hasAnyDreams,
        )
    }

    @Test
    fun searchNarrowsToMatchingDreamsAndFlagsItselfActive() = runTest {
        seed()
        val viewModel = newViewModel()
        viewModel.awaitUi { it.isLoaded && it.dreams.size == 3 }

        viewModel.setSearchQuery("forest")

        val state = viewModel.awaitUi { it.isSearchActive && it.dreams.size == 1 }
        assertEquals("Chased", state.dreams.single().dream.title)
    }

    @Test
    fun searchMatchesOnPrefixSoPartialWordsWork() = runTest {
        seed()
        val viewModel = newViewModel()
        viewModel.awaitUi { it.isLoaded && it.dreams.size == 3 }

        viewModel.setSearchQuery("fore")

        viewModel.awaitUi { it.dreams.size == 1 && it.dreams.single().dream.title == "Chased" }
    }

    @Test
    fun closingSearchRestoresTheFullList() = runTest {
        seed()
        val viewModel = newViewModel()
        viewModel.openSearch()
        viewModel.setSearchQuery("forest")
        viewModel.awaitUi { it.dreams.size == 1 }

        viewModel.closeSearch()

        val state = viewModel.awaitUi { it.dreams.size == 3 }
        assertFalse("a closed search must not keep filtering", state.isSearchActive)
        assertFalse(viewModel.isSearchBarVisible.value)
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun searchAndFiltersNarrowTogether() = runTest {
        seed()
        val viewModel = newViewModel()
        viewModel.awaitUi { it.isLoaded && it.dreams.size == 3 }

        // "Flying" and "Old house" share a tag; only one of them mentions the sea.
        viewModel.toggleTagFilter("flying")
        viewModel.setSearchQuery("sea")

        viewModel.awaitTitles("Flying")
    }

    @Test
    fun selectingAndDeletingMovesDreamsToTheBin() = runTest {
        seed()
        val viewModel = newViewModel()
        val loaded = viewModel.awaitUi { it.isLoaded && it.dreams.size == 3 }
        val doomed = loaded.dreams.take(2).map { it.dream.id }

        doomed.forEach(viewModel::toggleSelection)
        assertEquals(doomed.toSet(), viewModel.selectedIds.value)

        viewModel.deleteSelected()

        viewModel.awaitUi { it.dreams.size == 1 }
        assertTrue("selection clears once the deletion lands", viewModel.selectedIds.value.isEmpty())
        assertEquals(2, repository.observeBinnedCount().first())
    }

    @Test
    fun toggingTheSameDreamTwiceLeavesSelectionMode() = runTest {
        seed()
        val viewModel = newViewModel()
        val loaded = viewModel.awaitUi { it.isLoaded && it.dreams.size == 3 }
        val id = loaded.dreams.first().dream.id

        viewModel.toggleSelection(id)
        viewModel.toggleSelection(id)

        assertTrue("deselecting the last dream ends selection mode", viewModel.selectedIds.value.isEmpty())
    }

    @Test
    fun clearingFiltersBringsEveryDreamBack() = runTest {
        seed()
        val viewModel = newViewModel()
        viewModel.awaitUi { it.isLoaded && it.dreams.size == 3 }
        viewModel.setLucidOnly(true)
        viewModel.awaitTitles("Flying")

        viewModel.clearFilters()

        viewModel.awaitUi { it.dreams.size == 3 }
        assertEquals(0, viewModel.filters.value.activeCount)
    }
}
