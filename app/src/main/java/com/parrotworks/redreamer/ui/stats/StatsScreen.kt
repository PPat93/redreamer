@file:OptIn(ExperimentalLayoutApi::class)

package com.parrotworks.redreamer.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parrotworks.redreamer.R
import com.parrotworks.redreamer.data.Mood
import com.parrotworks.redreamer.repository.DreamStats
import com.parrotworks.redreamer.repository.MonthCount
import com.parrotworks.redreamer.ui.components.ComingSoonContent
import com.parrotworks.redreamer.ui.components.TagChip
import com.parrotworks.redreamer.ui.components.displayName
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    if (stats.totalDreams == 0) {
        ComingSoonContent(
            title = stringResource(R.string.stats_empty_title),
            body = stringResource(R.string.stats_empty_body),
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                value = stats.totalDreams.toString(),
                label = stringResource(R.string.stats_total_dreams_label),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = stats.currentStreakDays.toString(),
                label = stringResource(R.string.stats_streak_label),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = String.format(Locale.getDefault(), "%.1f", stats.averageClarity),
                label = stringResource(R.string.stats_avg_clarity_label),
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                value = "${stats.lucidPercent}%",
                label = stringResource(R.string.stats_lucid_label),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = "${stats.nightmarePercent}%",
                label = stringResource(R.string.stats_nightmare_label),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = "${stats.recurringPercent}%",
                label = stringResource(R.string.stats_recurring_label),
                modifier = Modifier.weight(1f),
            )
        }

        if (stats.topTags.isNotEmpty()) {
            StatsSection(title = stringResource(R.string.stats_top_tags_title)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    stats.topTags.forEach { tagCount ->
                        TagChip(name = "${tagCount.name} (${tagCount.count})")
                    }
                }
            }
        }

        if (stats.moodCounts.isNotEmpty()) {
            StatsSection(title = stringResource(R.string.stats_mood_distribution_title)) {
                val maxCount = stats.moodCounts.values.max()
                val sortedMoods = stats.moodCounts.entries.sortedByDescending { it.value }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortedMoods.forEach { (mood, count) ->
                        MoodBarRow(mood = mood, count = count, maxCount = maxCount)
                    }
                }
            }
        }

        StatsSection(title = stringResource(R.string.stats_dreams_per_month_title)) {
            DreamsPerMonthChart(stats.dreamsPerMonth)
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun MoodBarRow(mood: Mood, count: Int, maxCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = mood.displayName(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(90.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (count.toFloat() / maxCount).coerceIn(0.03f, 1f))
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(count.toString(), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DreamsPerMonthChart(months: List<MonthCount>) {
    val maxCount = months.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val maxBarHeight = 100.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxBarHeight + 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        months.forEach { monthCount ->
            val barHeight: Dp = if (monthCount.count == 0) {
                2.dp
            } else {
                (maxBarHeight.value * (monthCount.count.toFloat() / maxCount)).roundToInt().dp.coerceAtLeast(4.dp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(barHeight)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = monthCount.yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
