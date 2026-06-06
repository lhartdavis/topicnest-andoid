package com.techtower.meetingtranscriber.playback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.techtower.meetingtranscriber.util.formatClockTime

@Composable
fun PlayerBar(
    controller: AudioPlayerController,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = controller::togglePlayPause) {
                    Icon(
                        imageVector = if (controller.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (controller.isPlaying) "Pause" else "Play",
                    )
                }
                Text(
                    text = "${formatClockTime(controller.positionMillis)} / ${formatClockTime(controller.durationMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            val duration = controller.durationMillis.coerceAtLeast(1L)
            Slider(
                value = controller.positionMillis.coerceIn(0L, duration).toFloat(),
                onValueChange = { controller.seekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
            )
        }
    }
}
