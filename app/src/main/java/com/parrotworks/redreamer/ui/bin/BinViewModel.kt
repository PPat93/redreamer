package com.parrotworks.redreamer.ui.bin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parrotworks.redreamer.data.DreamWithTags
import com.parrotworks.redreamer.repository.DreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BinViewModel @Inject constructor(
    private val repository: DreamRepository,
) : ViewModel() {

    val binnedDreams: StateFlow<List<DreamWithTags>> = repository.observeBinnedDreams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(id: Long) {
        viewModelScope.launch { repository.restore(id) }
    }

    fun deleteForever(id: Long) {
        viewModelScope.launch { repository.deleteForever(id) }
    }

    fun emptyBin() {
        viewModelScope.launch { repository.emptyBin() }
    }
}
