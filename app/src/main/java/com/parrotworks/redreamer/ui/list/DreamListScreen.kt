package com.parrotworks.redreamer.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    if (dreams.isEmpty()) {
        EmptyDreamList(modifier = modifier.fillMaxSize())
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(dreams, key = { it.dream.id }) { dreamWithTags ->
                DreamCard(
                    dreamWithTags = dreamWithTags,
                    onClick = { onDreamClick(dreamWithTags.dream.id) },
                )
            }
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
