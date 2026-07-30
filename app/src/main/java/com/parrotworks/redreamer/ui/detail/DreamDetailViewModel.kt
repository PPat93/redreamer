package com.parrotworks.redreamer.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parrotworks.redreamer.data.DreamWithTags
import com.parrotworks.redreamer.repository.DreamRepository
import com.parrotworks.redreamer.ui.lock.AppLockController
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
    private val appLockController: AppLockController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * The share sheet stops our activity, which would otherwise re-lock the app. There's no result
     * callback for a chooser, so this excursion is closed out when the app returns to the
     * foreground rather than by an explicit end call.
     */
    fun onShareSheetOpened() = appLockController.beginSystemFlow()

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
