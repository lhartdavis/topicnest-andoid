package com.techtower.meetingtranscriber.transcription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techtower.meetingtranscriber.data.entities.TimestampSegmentEntity
import com.techtower.meetingtranscriber.data.entities.TimestampWordEntity
import com.techtower.meetingtranscriber.playback.PlayerBar
import com.techtower.meetingtranscriber.playback.rememberAudioPlayerController
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptDetailScreen(
    detail: TranscriptDetail?,
    onBack: () -> Unit,
    onShare: (TranscriptDetail) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(detail?.job?.displayName ?: "Transcript") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (detail?.job?.plainText != null) {
                        IconButton(onClick = { onShare(detail) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (detail == null) {
            Text(
                text = "Loading transcript...",
                modifier = Modifier.padding(padding).padding(16.dp),
            )
        } else {
            val controller = rememberAudioPlayerController(detail.job.audioUri)
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                PlayerBar(controller = controller)
                DetailBody(
                    detail = detail,
                    positionMillis = controller.positionMillis,
                    onNotesChange = onNotesChange,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DetailBody(
    detail: TranscriptDetail,
    positionMillis: Long,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var notes by remember(detail.job.id) { mutableStateOf(detail.job.notes.orEmpty()) }
    LaunchedEffect(notes) {
        delay(500)
        if (notes != detail.job.notes.orEmpty()) {
            onNotesChange(notes)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= 720.dp) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NotesBox(
                    notes = notes,
                    onNotesChange = { notes = it },
                    modifier = Modifier.weight(0.35f),
                )
                TranscriptContent(
                    detail = detail,
                    positionMillis = positionMillis,
                    modifier = Modifier.weight(0.65f),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NotesBox(
                    notes = notes,
                    onNotesChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                TranscriptContent(
                    detail = detail,
                    positionMillis = positionMillis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NotesBox(
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text("Notes") },
        minLines = 4,
        modifier = modifier,
    )
}

@Composable
private fun TranscriptContent(
    detail: TranscriptDetail,
    positionMillis: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Transcript",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        when {
            detail.words.isNotEmpty() -> WordTranscript(detail.words, positionMillis)
            detail.segments.isNotEmpty() -> SegmentTranscript(detail.segments, positionMillis)
            else -> Text(
                text = detail.job.plainText.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordTranscript(words: List<TimestampWordEntity>, positionMillis: Long) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        words.forEach { word ->
            val active = positionMillis in word.startMillis..word.endMillis
            Text(
                text = word.word,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.background,
                    )
                    .padding(horizontal = 3.dp, vertical = 1.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun SegmentTranscript(segments: List<TimestampSegmentEntity>, positionMillis: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        segments.forEach { segment ->
            val active = positionMillis in segment.startMillis..segment.endMillis
            Text(
                text = segment.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.background,
                    )
                    .padding(8.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
