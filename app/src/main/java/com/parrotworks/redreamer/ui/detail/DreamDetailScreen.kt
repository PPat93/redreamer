@file:OptIn(ExperimentalMaterial3Api::class)

package com.parrotworks.redreamer.ui.detail

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parrotworks.redreamer.R
import com.parrotworks.redreamer.data.Dream
import com.parrotworks.redreamer.data.DreamWithTags
import com.parrotworks.redreamer.ui.components.DreamChip
import com.parrotworks.redreamer.ui.components.MoodChip
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
fun DreamDetailScreen(
    onEditClick: () -> Unit,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: DreamDetailViewModel = hiltViewModel(),
) {
    val dreamWithTags by viewModel.dream.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dream_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                },
            )
        },
    ) { paddingValues ->
        val dreamWithTagsValue = dreamWithTags
        if (dreamWithTagsValue == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            DreamDetailContent(
                dreamWithTags = dreamWithTagsValue,
                modifier = Modifier.padding(paddingValues),
                onCopyFull = { clipboardManager.setText(AnnotatedString(dreamWithTagsValue.toFullText())) },
                onCopyTextOnly = { clipboardManager.setText(AnnotatedString(dreamWithTagsValue.dream.content)) },
                onShare = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, dreamWithTagsValue.toFullText())
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                },
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.dream_delete_confirm_title)) },
            text = { Text(stringResource(R.string.dream_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(onDeleted)
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
private fun DreamDetailContent(
    dreamWithTags: DreamWithTags,
    onCopyFull: () -> Unit,
    onCopyTextOnly: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dream = dreamWithTags.dream

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(dream.title.ifBlank { "Untitled dream" }, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = dream.dreamDate.format(dateFormatter),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (dream.moods.isNotEmpty() || dreamWithTags.tags.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                dream.moods.forEach { mood -> MoodChip(mood) }
                dreamWithTags.tags.forEach { tag -> DreamChip(text = tag.name) }
            }
        }

        DreamStatsSection(dream)

        HorizontalDivider()

        Text(dream.content, style = MaterialTheme.typography.bodyLarge)

        if (dream.notes.isNotBlank()) {
            HorizontalDivider()
            Text(stringResource(R.string.dream_field_notes), style = MaterialTheme.typography.labelLarge)
            Text(dream.notes, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onCopyFull) { Text(stringResource(R.string.action_copy)) }
            OutlinedButton(onClick = onCopyTextOnly) { Text(stringResource(R.string.action_copy_text_only)) }
            OutlinedButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_share))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DreamStatsSection(dream: Dream) {
    Column {
        Text(
            stringResource(R.string.dream_detail_clarity_value, dream.clarity),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (dream.isLucid) {
            Text(
                stringResource(R.string.dream_detail_lucidity_value, dream.lucidity ?: 0),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (dream.isNightmare) {
            Text(stringResource(R.string.dream_detail_nightmare_label), style = MaterialTheme.typography.bodyMedium)
        }
        if (dream.isRecurring) {
            Text(stringResource(R.string.dream_detail_recurring_label), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun DreamWithTags.toFullText(): String = buildString {
    appendLine(dream.title.ifBlank { "Untitled dream" })
    appendLine(dream.dreamDate.format(dateFormatter))
    appendLine()
    appendLine(dream.content)
    if (dream.notes.isNotBlank()) {
        appendLine()
        appendLine("Notes:")
        appendLine(dream.notes)
    }
    if (tags.isNotEmpty()) {
        appendLine()
        appendLine("Tags: " + tags.joinToString(", ") { it.name })
    }
}
