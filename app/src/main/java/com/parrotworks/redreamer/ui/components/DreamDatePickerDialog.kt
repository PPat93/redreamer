package com.parrotworks.redreamer.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.parrotworks.redreamer.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * [minDate] and [maxDate] grey out impossible choices rather than letting the user pick something
 * invalid and be quietly shown nothing — used by the filter sheet to keep a date range in order.
 *
 * Dates cross the Material3 boundary as UTC epoch millis, which is what its API specifies, so the
 * conversions here use [ZoneOffset.UTC] deliberately: reading the result back in the device's zone
 * is the classic way to land a day early or late.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamDatePickerDialog(
    initialDate: LocalDate,
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
) {
    val selectableDates = remember(minDate, maxDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return (minDate == null || !date.isBefore(minDate)) &&
                    (maxDate == null || !date.isAfter(maxDate))
            }
        }
    }

    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = selectableDates,
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismissRequest()
            }) {
                // "OK", not "Save" — picking a date doesn't commit anything on its own.
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    ) {
        DatePicker(state = state)
    }
}
