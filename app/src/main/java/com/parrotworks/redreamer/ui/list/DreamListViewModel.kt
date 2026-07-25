package com.parrotworks.redreamer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parrotworks.redreamer.data.DreamWithTags
import com.parrotworks.redreamer.data.Tag
import com.parrotworks.redreamer.repository.DreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DreamListUiState(
    val dreams: List<DreamWithTags> = emptyList(),
    /** Whether any live dreams exist at all, regardless of filters — distinguishes a truly
     * empty journal from filters that just happen to match nothing. */
    val hasAnyDreams: Boolean = false,
)

@HiltViewModel
class DreamListViewModel @Inject constructor(
    private val repository: DreamRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(DreamListFilters())
    val filters: StateFlow<DreamListFilters> = _filters.asStateFlow()

    val availableTags: StateFlow<List<Tag>> = repository.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<DreamListUiState> = combine(
        repository.observeLiveDreams(),
        _filters,
    ) { allDreams, filters ->
        DreamListUiState(
            dreams = applyFilters(allDreams, filters),
            hasAnyDreams = allDreams.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DreamListUiState())

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    init {
        // Bin entries older than 30 days are purged for good; this is the one
        // launch-time check since the app has no background scheduler yet.
        viewModelScope.launch {
            repository.purgeExpiredFromBin()
        }
    }

    private fun applyFilters(dreams: List<DreamWithTags>, filters: DreamListFilters): List<DreamWithTags> {
        if (filters.activeCount == 0) return dreams
        return dreams.filter { dreamWithTags ->
            val dream = dreamWithTags.dream
            (filters.startDate == null || !dream.dreamDate.isBefore(filters.startDate)) &&
                (filters.endDate == null || !dream.dreamDate.isAfter(filters.endDate)) &&
                (filters.tagNames.isEmpty() || dreamWithTags.tags.any { it.name in filters.tagNames }) &&
                (!filters.lucidOnly || dream.isLucid) &&
                (!filters.nightmareOnly || dream.isNightmare) &&
                (!filters.recurringOnly || dream.isRecurring)
        }
    }

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
}
