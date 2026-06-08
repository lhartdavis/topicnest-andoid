package com.techtower.meetingtranscriber.discovery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.techtower.meetingtranscriber.data.entities.AudioSource
import com.techtower.meetingtranscriber.transcription.requiresDirectUploadWarning
import com.techtower.meetingtranscriber.transcription.willUseAutomaticMp3ProcessingBeforeTranscription
import com.techtower.meetingtranscriber.util.formatDuration
import com.techtower.meetingtranscriber.util.formatFileSize
import com.techtower.meetingtranscriber.util.formatModifiedTime

@Composable
fun DiscoveryScreen(
    state: DiscoveryUiState,
    hasMediaPermission: Boolean,
    maxDirectUploadBytes: Long,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onChooseFolder: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onTranscribeSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLargeFileWarning by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = if (state.selectedIds.isEmpty()) 24.dp else 112.dp,
            ),
        ) {
            item {
                DiscoveryHeader(
                    hasMediaPermission = hasMediaPermission,
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    onRequestPermission = onRequestPermission,
                    onChooseFolder = onChooseFolder,
                )
            }
            state.message?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item { SectionTitle("Recent large recordings") }
            if (state.priorityFiles.isEmpty()) {
                item { EmptyLine("No priority recordings found yet.") }
            } else {
                items(state.priorityFiles, key = { "priority-${it.id}" }) { file ->
                    AudioFileRow(
                        file = file,
                        selected = file.id in state.selectedIds,
                        maxDirectUploadBytes = maxDirectUploadBytes,
                        onToggleSelection = onToggleSelection,
                    )
                }
            }
            item { SectionTitle("Browse") }
            if (state.files.isEmpty()) {
                item {
                    EmptyLine("No audio files found in Music/Record/SoundRecord. Tap Refresh or choose the folder manually.")
                }
            } else {
                items(state.files, key = { "browse-${it.id}" }) { file ->
                    AudioFileRow(
                        file = file,
                        selected = file.id in state.selectedIds,
                        maxDirectUploadBytes = maxDirectUploadBytes,
                        onToggleSelection = onToggleSelection,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.selectedIds.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${state.selectedIds.size} selected")
                    Button(
                        onClick = {
                            if (state.selectedFiles.any { requiresDirectUploadWarning(it.sizeBytes, maxDirectUploadBytes) }) {
                                showLargeFileWarning = true
                            } else {
                                onTranscribeSelected()
                            }
                        },
                    ) {
                        Text("Transcribe selected")
                    }
                }
            }
        }
    }

    if (showLargeFileWarning) {
        AlertDialog(
            onDismissRequest = { showLargeFileWarning = false },
            title = { Text("Large upload") },
            text = {
                Text("One or more files may exceed the ${formatFileSize(maxDirectUploadBytes)} JSON upload budget after base64 encoding. The app will convert and chunk audio automatically, but processing can take longer.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLargeFileWarning = false
                        onTranscribeSelected()
                    },
                ) {
                    Text("Attempt anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLargeFileWarning = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun DiscoveryHeader(
    hasMediaPermission: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onChooseFolder: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Discovery",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        if (!hasMediaPermission) {
            OutlinedButton(onClick = onRequestPermission) {
                Text("Grant audio access")
            }
        }
        ElevatedButton(onClick = onChooseFolder) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choose recorder folder")
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun EmptyLine(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun AudioFileRow(
    file: DiscoveredAudioFile,
    selected: Boolean,
    maxDirectUploadBytes: Long,
    onToggleSelection: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection(file.id) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggleSelection(file.id) },
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.displayName,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOf(
                        formatDuration(file.durationMillis),
                        formatFileSize(file.sizeBytes),
                        formatModifiedTime(file.modifiedEpochMillis),
                    ).joinToString(" | "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = listOfNotNull(file.mimeType, file.source.label()).joinToString(" | "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (file.willUseAutomaticMp3Processing(maxDirectUploadBytes)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Will convert to MP3 automatically",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun DiscoveredAudioFile.willUseAutomaticMp3Processing(
    maxDirectUploadBytes: Long,
): Boolean =
    willUseAutomaticMp3ProcessingBeforeTranscription(
        displayName = displayName,
        mimeType = mimeType,
        rawBytes = sizeBytes,
        maxDirectUploadBytes = maxDirectUploadBytes,
    )

private fun AudioSource.label(): String =
    when (this) {
        AudioSource.MEDIASTORE -> "MediaStore"
        AudioSource.SAF -> "Folder"
    }
