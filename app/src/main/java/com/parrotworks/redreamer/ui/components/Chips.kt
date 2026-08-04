package com.parrotworks.redreamer.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.parrotworks.redreamer.data.Mood

/** Static, non-interactive label chip — read-only display of moods and tags in cards. */
@Composable
private fun LabelChip(
    text: String,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * Tags read as pills with a leading `#`. Shape and prefix differ from [MoodChip], not just colour,
 * so the two stay distinguishable without relying on colour vision.
 */
@Composable
fun TagChip(name: String, modifier: Modifier = Modifier) {
    LabelChip(
        text = "#$name",
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
    )
}

/** Moods are square-ish and use the tertiary palette, visually opposite [TagChip]. */
@Composable
fun MoodChip(mood: Mood, modifier: Modifier = Modifier) {
    LabelChip(
        text = mood.displayName(),
        shape = MaterialTheme.shapes.small,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier,
    )
}

fun Mood.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
