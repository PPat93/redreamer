@file:OptIn(ExperimentalMaterial3Api::class)

package com.parrotworks.redreamer.ui.tags

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parrotworks.redreamer.R
import com.parrotworks.redreamer.data.TagWithUsage
import com.parrotworks.redreamer.ui.components.ComingSoonContent

@Composable
fun TagManagementScreen(
    onBack: () -> Unit,
    viewModel: TagManagementViewModel = hiltViewModel(),
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    var renamingTag by remember { mutableStateOf<TagWithUsage?>(null) }
    var deletingTag by remember { mutableStateOf<TagWithUsage?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tag_management_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.tag_add_new))
            }
        },
    ) { paddingValues ->
        if (tags.isEmpty()) {
            ComingSoonContent(
                title = stringResource(R.string.tag_management_empty_title),
                body = stringResource(R.string.tag_management_empty_body),
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tags, key = { it.id }) { tag ->
                    TagRow(
                        tag = tag,
                        onRename = { renamingTag = tag },
                        onDelete = { deletingTag = tag },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        TagNameDialog(
            title = stringResource(R.string.tag_add_dialog_title),
            initialName = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                showAddDialog = false
                viewModel.addTag(name)
            },
        )
    }

    renamingTag?.let { tag ->
        TagNameDialog(
            title = stringResource(R.string.tag_rename_dialog_title),
            initialName = tag.name,
            onDismiss = { renamingTag = null },
            onConfirm = { newName ->
                renamingTag = null
                viewModel.renameOrMerge(tag.id, newName)
            },
        )
    }

    deletingTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { deletingTag = null },
            title = { Text(stringResource(R.string.tag_delete_confirm_title)) },
            text = {
                Text(
                    when (tag.usageCount) {
                        0 -> stringResource(R.string.tag_delete_confirm_body_none)
                        1 -> stringResource(R.string.tag_delete_confirm_body_one)
                        else -> stringResource(R.string.tag_delete_confirm_body_many, tag.usageCount)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    deletingTag = null
                    viewModel.deleteTag(tag.id)
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTag = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun TagRow(tag: TagWithUsage, onRename: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tag.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = when (tag.usageCount) {
                        0 -> stringResource(R.string.tag_usage_zero)
                        1 -> stringResource(R.string.tag_usage_one)
                        else -> stringResource(R.string.tag_usage_many, tag.usageCount)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_rename))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        }
    }
}

@Composable
private fun TagNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
