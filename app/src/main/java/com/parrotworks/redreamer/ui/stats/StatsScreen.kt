package com.parrotworks.redreamer.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.parrotworks.redreamer.R
import com.parrotworks.redreamer.ui.components.ComingSoonContent

@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    ComingSoonContent(
        title = stringResource(R.string.stats_coming_soon_title),
        body = stringResource(R.string.stats_coming_soon_body),
        modifier = modifier,
    )
}
