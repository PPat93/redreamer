package com.parrotworks.redreamer.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parrotworks.redreamer.repository.DreamRepository
import com.parrotworks.redreamer.repository.DreamStats
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StatsViewModel @Inject constructor(
    repository: DreamRepository,
) : ViewModel() {

    val stats: StateFlow<DreamStats> = repository.observeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DreamStats.EMPTY)
}
