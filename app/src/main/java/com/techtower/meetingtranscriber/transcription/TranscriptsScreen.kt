package com.techtower.meetingtranscriber.transcription

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.techtower.meetingtranscriber.data.entities.TranscriptJobEntity
import com.techtower.meetingtranscriber.data.entities.TranscriptStatus
import com.techtower.meetingtranscriber.util.formatDuration
import com.techtower.meetingtranscriber.util.formatFileSize
import com.techtower.meetingtranscriber.util.formatModifiedTime
import com.techtower.meetingtranscriber.util.shortPreview

@Composable
fun TranscriptsScreen(
    jobs: List<TranscriptJobEntity>,
    onOpenDetail: (Long) -> Unit,
    onShare: (TranscriptJobEntity) -> Unit,
    onRetry: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            Text(
                text = "Transcripts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (jobs.isEmpty()) {
            item {
                Text(
                    text = "No transcription jobs yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(jobs, key = { it.id }) { job ->
                TranscriptJobRow(
                    job = job,
                    onOpenDetail = onOpenDetail,
                    onShare = onShare,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun TranscriptJobRow(
    job: TranscriptJobEntity,
    onOpenDetail: (Long) -> Unit,
    onShare: (TranscriptJobEntity) -> Unit,
    onRetry: (Long) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onOpenDetail(job.id) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = job.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusChip(job.status)
                }
                Text(
                    text = "${formatDuration(job.durationMillis)} | ${formatFileSize(job.sizeBytes)} | ${formatModifiedTime(job.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (job.status == TranscriptStatus.TRANSCRIBED) {
                    Text(
                        text = shortPreview(job.plainText),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (job.status == TranscriptStatus.FAILED && job.errorMessage != null) {
                    Text(
                        text = job.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (job.status == TranscriptStatus.TRANSCRIBED) {
                IconButton(onClick = { onShare(job) }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }
            if (job.status == TranscriptStatus.FAILED) {
                IconButton(onClick = { onRetry(job.id) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: TranscriptStatus) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = when (status) {
                    TranscriptStatus.QUEUED -> "Queued"
                    TranscriptStatus.PROCESSING -> "Processing"
                    TranscriptStatus.TRANSCRIBED -> "Transcribed"
                    TranscriptStatus.FAILED -> "Failed"
                },
            )
        },
    )
}
