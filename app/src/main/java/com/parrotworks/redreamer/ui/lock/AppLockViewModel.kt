package com.parrotworks.redreamer.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parrotworks.redreamer.data.prefs.AppPreferences
import com.parrotworks.redreamer.repository.MaintenanceRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Thin lifecycle-facing wrapper over [AppLockController]. Also the natural home for app-launch
 * housekeeping, since it's created once when the activity starts.
 */
@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val controller: AppLockController,
    preferences: AppPreferences,
    maintenanceRunner: MaintenanceRunner,
) : ViewModel() {

    /** Null until the stored preference has actually loaded, so we never flash unlocked content. */
    val lockEnabled: StateFlow<Boolean?> = preferences.appLockEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val unlocked: StateFlow<Boolean> = controller.unlocked

    val isInSystemFlow: Boolean get() = controller.isInSystemFlow

    init {
        viewModelScope.launch { maintenanceRunner.runStartupTasks() }
    }

    fun onUnlocked() = controller.unlock()

    fun relock() = controller.relock()

    fun beginSystemFlow() = controller.beginSystemFlow()

    fun endSystemFlow() = controller.endSystemFlow()

    fun onReturnedToForeground() = controller.onReturnedToForeground()
}
