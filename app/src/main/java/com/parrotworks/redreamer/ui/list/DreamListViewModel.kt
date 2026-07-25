package com.parrotworks.redreamer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parrotworks.redreamer.data.DreamWithTags
import com.parrotworks.redreamer.repository.DreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DreamListViewModel @Inject constructor(
    private val repository: DreamRepository,
) : ViewModel() {

    val dreams: StateFlow<List<DreamWithTags>> = repository.observeLiveDreams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    init {
        // Bin entries older than 30 days are purged for good; this is the one
        // launch-time check since the app has no background scheduler yet.
        viewModelScope.launch {
            repository.purgeExpiredFromBin()
        }
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
