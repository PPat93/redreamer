package com.parrotworks.redreamer.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parrotworks.redreamer.R
import com.parrotworks.redreamer.ui.components.DreamCard

/** The Dreams tab content within [com.parrotworks.redreamer.ui.home.HomeScreen] — no Scaffold/TopBar/FAB of its own. */
@Composable
fun DreamListContent(
    onDreamClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DreamListViewModel = hiltViewModel(),
) {
    val dreams by viewModel.dreams.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode = selectedIds.isNotEmpty()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        if (isSelectionMode) {
            SelectionActionBar(
                selectedCount = selectedIds.size,
                onClear = viewModel::clearSelection,
                onDeleteClick = { showDeleteConfirm = true },
            )
        }

        if (dreams.isEmpty()) {
            EmptyDreamList(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(dreams, key = { it.dream.id }) { dreamWithTags ->
                    val id = dreamWithTags.dream.id
                    DreamCard(
                        dreamWithTags = dreamWithTags,
                        selected = id in selectedIds,
                        onClick = {
                            if (isSelectionMode) viewModel.toggleSelection(id) else onDreamClick(id)
                        },
                        onLongClick = { viewModel.toggleSelection(id) },
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    if (count == 1) {
                        stringResource(R.string.dream_delete_confirm_title)
                    } else {
                        stringResource(R.string.dream_mass_delete_confirm_title, count)
                    },
                )
            },
            text = {
                Text(
                    if (count == 1) {
                        stringResource(R.string.dream_delete_confirm_body)
                    } else {
                        stringResource(R.string.dream_mass_delete_confirm_body)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteSelected()
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    onClear: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClear) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
        }
        Text(
            text = stringResource(R.string.dream_selected_count, selectedCount),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
        }
    }
}

@Composable
private fun EmptyDreamList(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.dream_list_empty_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.dream_list_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
