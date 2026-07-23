package com.parrotworks.redreamer.ui.list

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
class DreamListViewModel @Inject constructor(
    private val repository: DreamRepository,
) : ViewModel() {

    val dreams: StateFlow<List<DreamWithTags>> = repository.observeLiveDreams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Bin entries older than 30 days are purged for good; this is the one
        // launch-time check since the app has no background scheduler yet.
        viewModelScope.launch {
            repository.purgeExpiredFromBin()
        }
    }
}
