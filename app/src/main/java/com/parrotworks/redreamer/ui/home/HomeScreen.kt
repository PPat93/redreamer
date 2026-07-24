package com.parrotworks.redreamer.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.parrotworks.redreamer.R
import com.parrotworks.redreamer.ui.list.DreamListContent
import com.parrotworks.redreamer.ui.settings.SettingsScreen
import com.parrotworks.redreamer.ui.stats.StatsScreen

private const val TAB_DREAMS = 0
private const val TAB_STATS = 1
private const val TAB_SETTINGS = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDreamClick: (Long) -> Unit,
    onAddDreamClick: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(TAB_DREAMS) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { AppBrandTitle() })
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == TAB_DREAMS,
                        onClick = { selectedTab = TAB_DREAMS },
                        text = { Text(stringResource(R.string.dream_list_title)) },
                    )
                    Tab(
                        selected = selectedTab == TAB_STATS,
                        onClick = { selectedTab = TAB_STATS },
                        text = { Text(stringResource(R.string.tab_stats)) },
                    )
                    Tab(
                        selected = selectedTab == TAB_SETTINGS,
                        onClick = { selectedTab = TAB_SETTINGS },
                        text = { Text(stringResource(R.string.tab_settings)) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == TAB_DREAMS) {
                FloatingActionButton(onClick = onAddDreamClick) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.dream_add_new))
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (selectedTab) {
                TAB_DREAMS -> DreamListContent(onDreamClick = onDreamClick)
                TAB_STATS -> StatsScreen()
                else -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun AppBrandTitle() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape)) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_background),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
            )
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
    }
}
