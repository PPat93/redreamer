package com.parrotworks.redreamer.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parrotworks.redreamer.R
import com.parrotworks.redreamer.data.backup.BackupManager
import com.parrotworks.redreamer.ui.lock.canUseAppLock

@Composable
fun SettingsScreen(
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val appLockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()

    val canUseAppLock = remember { context.canUseAppLock() }

    // The picker result also arrives when the user cancels, so the excursion is always closed out.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupManager.EXPORT_MIME_TYPE),
    ) { uri ->
        viewModel.onSystemPickerClosed()
        uri?.let(viewModel::exportTo)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        viewModel.onSystemPickerClosed()
        uri?.let(viewModel::importFrom)
    }

    LaunchedEffect(result) {
        val current = result ?: return@LaunchedEffect
        val message = when (current) {
            is BackupResult.Exported -> context.getString(R.string.backup_exported, current.dreamCount)
            is BackupResult.Imported -> if (current.skippedCount > 0) {
                context.getString(R.string.backup_imported_with_skips, current.dreamCount, current.skippedCount)
            } else {
                context.getString(R.string.backup_imported, current.dreamCount)
            }
            BackupResult.Failed -> context.getString(R.string.backup_failed)
        }
        onShowMessage(message)
        viewModel.consumeResult()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (isBusy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        SectionHeader(stringResource(R.string.settings_section_data))

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_export_title)) },
            supportingContent = { Text(stringResource(R.string.settings_export_body)) },
            leadingContent = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isBusy) {
                    viewModel.onSystemPickerOpened()
                    exportLauncher.launch(viewModel.suggestedExportFileName())
                },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_import_title)) },
            supportingContent = { Text(stringResource(R.string.settings_import_body)) },
            leadingContent = { Icon(Icons.Filled.FileUpload, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isBusy) {
                    viewModel.onSystemPickerOpened()
                    importLauncher.launch(BackupManager.IMPORT_MIME_TYPES)
                },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_auto_backup_title)) },
            supportingContent = { Text(stringResource(R.string.settings_auto_backup_body)) },
            leadingContent = { Icon(Icons.Filled.Backup, contentDescription = null) },
            trailingContent = {
                Switch(checked = autoBackupEnabled, onCheckedChange = null)
            },
            // Whole row toggles; a switch alone is a small target and the label did nothing.
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = autoBackupEnabled,
                    onValueChange = viewModel::setAutoBackupEnabled,
                    role = Role.Switch,
                ),
        )

        HorizontalDivider()
        SectionHeader(stringResource(R.string.settings_section_privacy))

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_app_lock_title)) },
            supportingContent = {
                Text(
                    if (canUseAppLock) {
                        stringResource(R.string.settings_app_lock_body)
                    } else {
                        stringResource(R.string.settings_app_lock_unavailable)
                    },
                )
            },
            leadingContent = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingContent = {
                Switch(
                    checked = appLockEnabled,
                    onCheckedChange = null,
                    // Always allow switching *off*. If device security was removed after enabling,
                    // a disabled switch would leave the setting stuck on with no way to clear it.
                    enabled = canUseAppLock || appLockEnabled,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = appLockEnabled,
                    enabled = canUseAppLock || appLockEnabled,
                    onValueChange = viewModel::setAppLockEnabled,
                    role = Role.Switch,
                ),
        )

        HorizontalDivider()
        // Plain text rather than EmptyStateContent: that component fills available height,
        // which has no meaning inside a vertically scrolling column.
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.settings_notifications_coming_soon_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                stringResource(R.string.settings_notifications_coming_soon_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}
