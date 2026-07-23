package com.parrotworks.redreamer.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parrotworks.redreamer.data.DreamWithTags
import com.parrotworks.redreamer.repository.DreamRepository
import com.parrotworks.redreamer.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DreamDetailViewModel @Inject constructor(
    private val repository: DreamRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val dreamId: Long = checkNotNull(savedStateHandle[Destinations.ARG_DREAM_ID])

    val dream: StateFlow<DreamWithTags?> = repository.observeDream(dreamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.softDelete(dreamId)
            onDeleted()
        }
    }
}
