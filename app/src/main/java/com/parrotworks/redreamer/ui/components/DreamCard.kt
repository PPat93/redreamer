package com.parrotworks.redreamer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.parrotworks.redreamer.data.DreamWithTags
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
fun DreamCard(
    dreamWithTags: DreamWithTags,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dream = dreamWithTags.dream

    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Text(
                    text = dream.title.ifBlank { "Untitled dream" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (dream.isLucid) {
                    Icon(Icons.Filled.Bedtime, contentDescription = "Lucid dream", modifier = Modifier.size(18.dp))
                }
                if (dream.isNightmare) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.WarningAmber, contentDescription = "Nightmare", modifier = Modifier.size(18.dp))
                }
                if (dream.isRecurring) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.Repeat, contentDescription = "Recurring dream", modifier = Modifier.size(18.dp))
                }
            }

            Text(
                text = dream.dreamDate.format(dateFormatter),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (dream.content.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = dream.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (dreamWithTags.tags.isNotEmpty() || dream.moods.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    dream.moods.forEach { mood -> MoodChip(mood) }
                    dreamWithTags.tags.forEach { tag -> DreamChip(text = tag.name) }
                }
            }
        }
    }
}
