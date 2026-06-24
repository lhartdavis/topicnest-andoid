package com.techtower.meetingtranscriber.transcription

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TranscriptsScreen(
    jobs: List<TranscriptJobEntity>,
    onOpenDetail: (Long) -> Unit,
    onShare: (TranscriptJobEntity) -> Unit,
    onRetry: (Long) -> Unit,
    onRetryFailed: () -> Unit,
    onRestartQueue: () -> Unit,
    onFailActiveJobs: () -> Unit,
    onDelete: (Long) -> Unit,
    searchQuery: String,
    isCompactList: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleCompactList: () -> Unit,
    actionMessage: String?,
    modifier: Modifier = Modifier,
) {
    var pendingDeleteJob by remember { mutableStateOf<TranscriptJobEntity?>(null) }
    val failedCount = jobs.count { it.status == TranscriptStatus.FAILED }
    val activeCount = jobs.count { it.status == TranscriptStatus.QUEUED || it.status == TranscriptStatus.PROCESSING }
    val displayJobs = rankTranscriptJobs(jobs, searchQuery)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        item {
            TranscriptsHeader(
                searchQuery = searchQuery,
                isCompactList = isCompactList,
                onSearchQueryChange = onSearchQueryChange,
                onToggleCompactList = onToggleCompactList,
            )
        }
        if (failedCount > 0 || activeCount > 0 || actionMessage != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (failedCount > 0) {
                            Button(onClick = onRetryFailed) {
                                Text("Retry failed ($failedCount)")
                            }
                        }
                        if (activeCount > 0) {
                            OutlinedButton(onClick = onRestartQueue) {
                                Text("Restart queue ($activeCount)")
                            }
                            OutlinedButton(onClick = onFailActiveJobs) {
                                Text("Stop stuck ($activeCount)")
                            }
                        }
                    }
                    actionMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (jobs.isEmpty()) {
            item {
                Text(
                    text = "No transcription jobs yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (displayJobs.isEmpty()) {
            item {
                Text(
                    text = "No transcripts match this search.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(displayJobs, key = { it.id }) { job ->
                TranscriptJobRow(
                    job = job,
                    compact = isCompactList,
                    onOpenDetail = onOpenDetail,
                    onShare = onShare,
                    onRetry = onRetry,
                    onRequestDelete = { pendingDeleteJob = job },
                )
            }
        }
    }

    pendingDeleteJob?.let { job ->
        AlertDialog(
            onDismissRequest = { pendingDeleteJob = null },
            title = { Text("Delete transcript?") },
            text = {
                Text("This removes the transcript, notes, timestamps, and job record. The original audio file stays on your device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteJob = null
                        onDelete(job.id)
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteJob = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun TranscriptsHeader(
    searchQuery: String,
    isCompactList: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleCompactList: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Transcripts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onToggleCompactList) {
                Icon(
                    Icons.AutoMirrored.Filled.ViewList,
                    contentDescription = if (isCompactList) "Use detailed list" else "Use compact list",
                    tint = if (isCompactList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search transcripts") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        )
    }
}

@Composable
private fun TranscriptJobRow(
    job: TranscriptJobEntity,
    compact: Boolean,
    onOpenDetail: (Long) -> Unit,
    onShare: (TranscriptJobEntity) -> Unit,
    onRetry: (Long) -> Unit,
    onRequestDelete: () -> Unit,
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
            if (compact) {
                Text(
                    text = formatDuration(job.durationMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(10.dp))
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
            } else {
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
            IconButton(onClick = onRequestDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete transcript")
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
