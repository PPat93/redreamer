package com.parrotworks.redreamer.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parrotworks.redreamer.data.prefs.AppPreferences
import com.parrotworks.redreamer.repository.MaintenanceRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns whether the journal is currently readable. Also the natural home for app-launch
 * housekeeping, since it's created once when the activity starts.
 */
@HiltViewModel
class AppLockViewModel @Inject constructor(
    preferences: AppPreferences,
    maintenanceRunner: MaintenanceRunner,
) : ViewModel() {

    /** Null until the stored preference has actually loaded, so we never flash unlocked content. */
    val lockEnabled: StateFlow<Boolean?> = preferences.appLockEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    /**
     * True while the OS authentication UI is showing. Survives here rather than in composition
     * because the device-credential prompt is a separate activity, which tears the composable down.
     */
    var isAuthenticating: Boolean = false

    init {
        viewModelScope.launch { maintenanceRunner.runStartupTasks() }
    }

    fun onUnlocked() {
        _unlocked.value = true
    }

    /** Called when the app leaves the foreground so it re-locks on return. */
    fun relock() {
        _unlocked.value = false
    }
}
