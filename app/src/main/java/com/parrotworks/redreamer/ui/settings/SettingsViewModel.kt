package com.parrotworks.redreamer.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parrotworks.redreamer.data.backup.BackupManager
import com.parrotworks.redreamer.data.prefs.AppPreferences
import com.parrotworks.redreamer.repository.DreamRepository
import com.parrotworks.redreamer.ui.lock.AppLockController
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-shot outcome of an export/import, surfaced as a snackbar then cleared. */
sealed interface BackupResult {
    data class Exported(val dreamCount: Int) : BackupResult
    data class Imported(val dreamCount: Int, val skippedCount: Int) : BackupResult
    data object Failed : BackupResult
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: DreamRepository,
    private val backupManager: BackupManager,
    private val preferences: AppPreferences,
    private val appLockController: AppLockController,
) : ViewModel() {

    /** The file picker is a separate activity; without this the app would re-lock behind it. */
    fun onSystemPickerOpened() = appLockController.beginSystemFlow()

    fun onSystemPickerClosed() = appLockController.endSystemFlow()

    val appLockEnabled: StateFlow<Boolean> = preferences.appLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val autoBackupEnabled: StateFlow<Boolean> = preferences.autoBackupEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _result = MutableStateFlow<BackupResult?>(null)
    val result: StateFlow<BackupResult?> = _result.asStateFlow()

    /** Dated so successive exports don't silently overwrite or pile up as "file (1).json". */
    fun suggestedExportFileName(): String = "redreamer-export-${LocalDate.now()}.json"

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            _result.value = runCatching {
                val snapshot = repository.exportSnapshot()
                backupManager.writeToUri(uri, snapshot)
                BackupResult.Exported(snapshot.dreams.size)
            }.getOrElse { BackupResult.Failed }
            _isBusy.value = false
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            _result.value = runCatching {
                val backup = backupManager.readFromUri(uri)
                val result = repository.importBackup(backup)
                BackupResult.Imported(dreamCount = result.imported, skippedCount = result.skipped)
            }.getOrElse { BackupResult.Failed }
            _isBusy.value = false
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setAppLockEnabled(enabled) }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoBackupEnabled(enabled) }
    }

    fun consumeResult() {
        _result.value = null
    }
}
