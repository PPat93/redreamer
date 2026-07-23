@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.parrotworks.redreamer.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parrotworks.redreamer.R
import com.parrotworks.redreamer.data.Mood
import com.parrotworks.redreamer.ui.components.displayName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
fun DreamEditorScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: DreamEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isEditingExisting) {
                                R.string.dream_editor_title_edit
                            } else {
                                R.string.dream_editor_title_new
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveNow(onSaved) }) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { paddingValues ->
        if (!uiState.isReady) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text(stringResource(R.string.dream_field_title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            DreamDatePickerField(date = uiState.dreamDate, onDateChange = viewModel::onDreamDateChange)

            OutlinedTextField(
                value = uiState.content,
                onValueChange = viewModel::onContentChange,
                label = { Text(stringResource(R.string.dream_field_content)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
            )

            Column {
                Text(stringResource(R.string.dream_field_moods), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Mood.entries.forEach { mood ->
                        FilterChip(
                            selected = mood in uiState.moods,
                            onClick = { viewModel.onMoodToggle(mood) },
                            label = { Text(mood.displayName()) },
                        )
                    }
                }
            }

            Column {
                Text(
                    text = stringResource(R.string.dream_field_clarity) + ": ${uiState.clarity}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = uiState.clarity.toFloat(),
                    onValueChange = { viewModel.onClarityChange(it.roundToInt()) },
                    valueRange = 0f..10f,
                    steps = 9,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = uiState.isLucid, onCheckedChange = viewModel::onLucidChange)
                Text(stringResource(R.string.dream_field_lucid))
            }

            if (uiState.isLucid) {
                Column {
                    Text(
                        text = stringResource(R.string.dream_field_lucidity) + ": ${uiState.lucidity}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Slider(
                        value = uiState.lucidity.toFloat(),
                        onValueChange = { viewModel.onLucidityChange(it.roundToInt()) },
                        valueRange = 0f..10f,
                        steps = 9,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = uiState.isNightmare, onCheckedChange = viewModel::onNightmareChange)
                Text(stringResource(R.string.dream_field_nightmare))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = uiState.isRecurring, onCheckedChange = viewModel::onRecurringChange)
                Text(stringResource(R.string.dream_field_recurring))
            }

            TagEditor(
                tagInput = uiState.tagInput,
                tags = uiState.tags,
                suggestions = remember(uiState.tagInput, uiState.tags, uiState.allTagNames) {
                    uiState.allTagNames.filter { candidate ->
                        uiState.tagInput.isNotBlank() &&
                            candidate.contains(uiState.tagInput, ignoreCase = true) &&
                            uiState.tags.none { existing -> existing.equals(candidate, ignoreCase = true) }
                    }.take(5)
                },
                onTagInputChange = viewModel::onTagInputChange,
                onAddTag = viewModel::onAddTag,
                onRemoveTag = viewModel::onRemoveTag,
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text(stringResource(R.string.dream_field_notes)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DreamDatePickerField(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    Column {
        Text(stringResource(R.string.dream_field_date), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        AssistChip(onClick = { showPicker = true }, label = { Text(date.format(dateFormatter)) })
    }

    if (showPicker) {
        val state = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            androidx.compose.material3.DatePicker(state = state)
        }
    }
}

@Composable
private fun TagEditor(
    tagInput: String,
    tags: List<String>,
    suggestions: List<String>,
    onTagInputChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
) {
    Column {
        Text(stringResource(R.string.dream_field_tags), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = tagInput,
            onValueChange = onTagInputChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { onAddTag(tagInput) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.dream_add_tag))
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAddTag(tagInput) }),
        )

        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                suggestions.forEach { name ->
                    AssistChip(onClick = { onAddTag(name) }, label = { Text(name) })
                }
            }
        }

        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { tagName ->
                    InputChip(
                        selected = false,
                        onClick = {},
                        label = { Text(tagName) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove $tagName",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onRemoveTag(tagName) },
                            )
                        },
                    )
                }
            }
        }
    }
}
