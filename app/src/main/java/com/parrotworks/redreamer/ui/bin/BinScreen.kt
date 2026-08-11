@file:OptIn(ExperimentalMaterial3Api::class)

package com.parrotworks.redreamer.ui.bin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parrotworks.redreamer.R
import com.parrotworks.redreamer.data.DreamWithTags
import com.parrotworks.redreamer.repository.DreamRepository
import com.parrotworks.redreamer.ui.components.EmptyStateContent
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
fun BinScreen(
    onBack: () -> Unit,
    viewModel: BinViewModel = hiltViewModel(),
) {
    val binnedDreams by viewModel.binnedDreams.collectAsStateWithLifecycle()
    var pendingDeleteForeverId by remember { mutableStateOf<Long?>(null) }
    var showEmptyBinConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bin_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (binnedDreams.isNotEmpty()) {
                        IconButton(onClick = { showEmptyBinConfirm = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.bin_empty_action))
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        if (binnedDreams.isEmpty()) {
            EmptyStateContent(
                title = stringResource(R.string.bin_empty_title),
                body = stringResource(R.string.bin_empty_body),
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(binnedDreams, key = { it.dream.id }) { dreamWithTags ->
                    BinnedDreamRow(
                        dreamWithTags = dreamWithTags,
                        onRestore = { viewModel.restore(dreamWithTags.dream.id) },
                        onDeleteForever = { pendingDeleteForeverId = dreamWithTags.dream.id },
                    )
                }
            }
        }
    }

    if (showEmptyBinConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyBinConfirm = false },
            title = { Text(stringResource(R.string.bin_empty_confirm_title)) },
            text = { Text(stringResource(R.string.bin_empty_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showEmptyBinConfirm = false
                    viewModel.emptyBin()
                }) {
                    Text(stringResource(R.string.bin_empty_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyBinConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    val deleteId = pendingDeleteForeverId
    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteForeverId = null },
            title = { Text(stringResource(R.string.bin_delete_forever_confirm_title)) },
            text = { Text(stringResource(R.string.bin_delete_forever_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteForeverId = null
                    viewModel.deleteForever(deleteId)
                }) {
                    Text(stringResource(R.string.action_delete_forever))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteForeverId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun BinnedDreamRow(
    dreamWithTags: DreamWithTags,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
) {
    val dream = dreamWithTags.dream
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dream.title.ifBlank { stringResource(R.string.dream_untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Date and a snippet of the text: deleting forever can't be undone, and several
                // untitled dreams would otherwise be indistinguishable from each other here.
                Text(
                    text = dream.dreamDate.format(dateFormatter),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (dream.content.isNotBlank()) {
                    Text(
                        text = dream.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = daysRemainingLabel(dream.deletedAt ?: Instant.now()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Filled.Restore, contentDescription = stringResource(R.string.action_restore))
            }
            IconButton(onClick = onDeleteForever) {
                Icon(Icons.Filled.DeleteForever, contentDescription = stringResource(R.string.action_delete_forever))
            }
        }
    }
}

@Composable
private fun daysRemainingLabel(deletedAt: Instant): String {
    val elapsedDays = Duration.between(deletedAt, Instant.now()).toDays()
    val remaining = (DreamRepository.BIN_RETENTION_DAYS - elapsedDays).coerceAtLeast(0)
    return when (remaining) {
        0L -> stringResource(R.string.bin_deletes_today)
        1L -> stringResource(R.string.bin_deletes_in_one_day)
        else -> stringResource(R.string.bin_deletes_in_days, remaining)
    }
}
