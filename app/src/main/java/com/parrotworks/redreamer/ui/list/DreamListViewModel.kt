package com.parrotworks.redreamer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parrotworks.redreamer.data.DreamWithTags
import com.parrotworks.redreamer.data.Tag
import com.parrotworks.redreamer.repository.DreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DreamListUiState(
    val dreams: List<DreamWithTags> = emptyList(),
    /** Whether any live dreams exist at all, regardless of filters/search — distinguishes a truly
     * empty journal from a search or filter that just happens to match nothing. */
    val hasAnyDreams: Boolean = false,
    val isSearchActive: Boolean = false,
    /** False until the first database emission, so no empty state is shown before we know anything. */
    val isLoaded: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DreamListViewModel @Inject constructor(
    private val repository: DreamRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(DreamListFilters())
    val filters: StateFlow<DreamListFilters> = _filters.asStateFlow()

    private val _isSearchBarVisible = MutableStateFlow(false)
    val isSearchBarVisible: StateFlow<Boolean> = _isSearchBarVisible.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val availableTags: StateFlow<List<Tag>> = repository.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val searchResultsOrAll: Flow<List<DreamWithTags>> = _searchQuery
        // Debounce only actual typing. A flat delay would also hold back the initial empty query,
        // leaving the list briefly showing its "no dreams yet" state on every launch.
        .debounce { query -> if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .flatMapLatest { query ->
            if (query.isBlank()) repository.observeLiveDreams() else repository.searchDreams(query)
        }

    val uiState: StateFlow<DreamListUiState> = combine(
        repository.observeLiveDreams(),
        searchResultsOrAll,
        _filters,
        _searchQuery,
    ) { allDreams, searchOrAllDreams, filters, query ->
        DreamListUiState(
            dreams = searchOrAllDreams.filteredBy(filters),
            hasAnyDreams = allDreams.isNotEmpty(),
            isSearchActive = query.isNotBlank(),
            isLoaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DreamListUiState())

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    fun setStartDate(date: LocalDate?) {
        _filters.update { it.copy(startDate = date) }
    }

    fun setEndDate(date: LocalDate?) {
        _filters.update { it.copy(endDate = date) }
    }

    fun setLucidOnly(value: Boolean) {
        _filters.update { it.copy(lucidOnly = value) }
    }

    fun setNightmareOnly(value: Boolean) {
        _filters.update { it.copy(nightmareOnly = value) }
    }

    fun setRecurringOnly(value: Boolean) {
        _filters.update { it.copy(recurringOnly = value) }
    }

    fun toggleTagFilter(tagName: String) {
        _filters.update {
            val newTags = if (tagName in it.tagNames) it.tagNames - tagName else it.tagNames + tagName
            it.copy(tagNames = newTags)
        }
    }

    fun clearFilters() {
        _filters.value = DreamListFilters()
    }

    fun openSearch() {
        _isSearchBarVisible.value = true
    }

    /** Closing the search bar also clears the query, so a stale search can't silently keep filtering the list. */
    fun closeSearch() {
        _isSearchBarVisible.value = false
        _searchQuery.value = ""
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Long-pressing an item toggles it whether selection mode is starting, continuing, or (if it
     * was the last item selected) ending; tapping an item while selection mode is already active
     * does the same.
     */
    fun toggleSelection(id: Long) {
        _selectedIds.update { current -> if (id in current) current - id else current + id }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.softDeleteAll(ids.toList())
            _selectedIds.value = emptySet()
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
