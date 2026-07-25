@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.parrotworks.redreamer.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.parrotworks.redreamer.R
import com.parrotworks.redreamer.data.Tag
import com.parrotworks.redreamer.ui.components.DreamDatePickerDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
fun FilterBottomSheet(
    filters: DreamListFilters,
    availableTags: List<Tag>,
    onDismiss: () -> Unit,
    onLucidToggle: (Boolean) -> Unit,
    onNightmareToggle: (Boolean) -> Unit,
    onRecurringToggle: (Boolean) -> Unit,
    onStartDateChange: (LocalDate?) -> Unit,
    onEndDateChange: (LocalDate?) -> Unit,
    onTagToggle: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.filters_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (filters.activeCount > 0) {
                    TextButton(onClick = onClearAll) {
                        Text(stringResource(R.string.filters_clear_all))
                    }
                }
            }

            Column {
                Text(stringResource(R.string.filters_dream_type), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filters.lucidOnly,
                        onClick = { onLucidToggle(!filters.lucidOnly) },
                        label = { Text(stringResource(R.string.filters_lucid_chip)) },
                    )
                    FilterChip(
                        selected = filters.nightmareOnly,
                        onClick = { onNightmareToggle(!filters.nightmareOnly) },
                        label = { Text(stringResource(R.string.filters_nightmare_chip)) },
                    )
                    FilterChip(
                        selected = filters.recurringOnly,
                        onClick = { onRecurringToggle(!filters.recurringOnly) },
                        label = { Text(stringResource(R.string.filters_recurring_chip)) },
                    )
                }
            }

            Column {
                Text(stringResource(R.string.filters_date_range), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateRangeChip(
                        label = stringResource(R.string.filters_from_date),
                        date = filters.startDate,
                        onClick = { showStartPicker = true },
                        onClear = { onStartDateChange(null) },
                    )
                    DateRangeChip(
                        label = stringResource(R.string.filters_to_date),
                        date = filters.endDate,
                        onClick = { showEndPicker = true },
                        onClear = { onEndDateChange(null) },
                    )
                }
            }

            if (availableTags.isNotEmpty()) {
                Column {
                    Text(stringResource(R.string.dream_field_tags), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableTags.forEach { tag ->
                            FilterChip(
                                selected = tag.name in filters.tagNames,
                                onClick = { onTagToggle(tag.name) },
                                label = { Text(tag.name) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        DreamDatePickerDialog(
            initialDate = filters.startDate ?: LocalDate.now(),
            onDismissRequest = { showStartPicker = false },
            onDateSelected = onStartDateChange,
        )
    }

    if (showEndPicker) {
        DreamDatePickerDialog(
            initialDate = filters.endDate ?: LocalDate.now(),
            onDismissRequest = { showEndPicker = false },
            onDateSelected = onEndDateChange,
        )
    }
}

@Composable
private fun DateRangeChip(
    label: String,
    date: LocalDate?,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    InputChip(
        selected = date != null,
        onClick = onClick,
        label = { Text(date?.format(dateFormatter) ?: label) },
        trailingIcon = if (date != null) {
            {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onClear),
                )
            }
        } else {
            null
        },
    )
}
