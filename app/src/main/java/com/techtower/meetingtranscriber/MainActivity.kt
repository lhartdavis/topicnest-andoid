package com.techtower.meetingtranscriber

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.techtower.meetingtranscriber.data.AppDatabase
import com.techtower.meetingtranscriber.discovery.AudioDiscoveryRepository
import com.techtower.meetingtranscriber.discovery.DiscoveryScreen
import com.techtower.meetingtranscriber.discovery.DiscoveryViewModel
import com.techtower.meetingtranscriber.discovery.MediaStoreAudioScanner
import com.techtower.meetingtranscriber.discovery.SafAudioScanner
import com.techtower.meetingtranscriber.settings.ApiKeyValidationSnapshot
import com.techtower.meetingtranscriber.settings.ApiKeyValidationStatus
import com.techtower.meetingtranscriber.settings.ApiKeyDialog
import com.techtower.meetingtranscriber.settings.OpenRouterKeyClient
import com.techtower.meetingtranscriber.settings.SettingsRepository
import com.techtower.meetingtranscriber.settings.SettingsViewModel
import com.techtower.meetingtranscriber.transcription.TranscriptDetailScreen
import com.techtower.meetingtranscriber.transcription.TranscriptDetailViewModel
import com.techtower.meetingtranscriber.transcription.TranscriptRepository
import com.techtower.meetingtranscriber.transcription.TranscriptsScreen
import com.techtower.meetingtranscriber.transcription.TranscriptsViewModel
import com.techtower.meetingtranscriber.ui.MeetingTranscriberTheme
import com.techtower.meetingtranscriber.util.buildShareIntent
import com.techtower.meetingtranscriber.util.requiredAudioPermission

class MainActivity : ComponentActivity() {
    private val database by lazy { AppDatabase.getInstance(this) }
    private val settingsRepository by lazy { SettingsRepository(this) }
    private val transcriptRepository by lazy { TranscriptRepository(this, database) }
    private val discoveryRepository by lazy {
        AudioDiscoveryRepository(
            audioFileDao = database.audioFileDao(),
            mediaStoreScanner = MediaStoreAudioScanner(this),
            safAudioScanner = SafAudioScanner(this),
            settingsRepository = settingsRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MeetingTranscriberTheme {
                MeetingTranscriberApp()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MeetingTranscriberApp() {
        val discoveryViewModel: DiscoveryViewModel = viewModel(
            factory = SimpleViewModelFactory {
                DiscoveryViewModel(discoveryRepository, transcriptRepository, settingsRepository)
            },
        )
        val transcriptsViewModel: TranscriptsViewModel = viewModel(
            factory = SimpleViewModelFactory { TranscriptsViewModel(transcriptRepository) },
        )
        val settingsViewModel: SettingsViewModel = viewModel(
            factory = SimpleViewModelFactory {
                SettingsViewModel(settingsRepository, OpenRouterKeyClient())
            },
        )
        val discoveryState by discoveryViewModel.uiState.collectAsStateWithLifecycle()
        val jobs by transcriptsViewModel.jobs.collectAsStateWithLifecycle()
        val transcriptActionMessage by transcriptsViewModel.actionMessage.collectAsStateWithLifecycle()
        val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }
        var detailJobId by rememberSaveable { mutableStateOf<Long?>(null) }
        var showApiKeyDialog by remember { mutableStateOf(false) }
        var hasMediaPermission by remember { mutableStateOf(isAudioPermissionGranted()) }

        val requestPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            hasMediaPermission = granted
            discoveryViewModel.refresh(granted)
        }
        val folderLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                settingsRepository.saveRecorderTreeUri(uri)
                discoveryViewModel.refresh(hasMediaPermission)
            }
        }

        LaunchedEffect(Unit) {
            discoveryViewModel.refresh(hasMediaPermission)
        }

        val currentDetailJobId = detailJobId
        if (currentDetailJobId != null) {
            val detailViewModel: TranscriptDetailViewModel = viewModel(
                key = "detail-$currentDetailJobId",
                factory = SimpleViewModelFactory {
                    TranscriptDetailViewModel(currentDetailJobId, transcriptRepository)
                },
            )
            val detail by detailViewModel.detail.collectAsStateWithLifecycle()
            TranscriptDetailScreen(
                detail = detail,
                onBack = { detailJobId = null },
                onShare = { transcriptDetail ->
                    shareTranscript(transcriptDetail.job.notes, transcriptDetail.job.plainText)
                },
                onNotesChange = detailViewModel::saveNotes,
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Meeting Transcriber") },
                        actions = {
                            IconButton(onClick = { showApiKeyDialog = true }) {
                                ApiKeyStatusIcon(settingsState.keyValidation)
                            }
                        },
                    )
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Search, contentDescription = null) },
                            label = { Text("Discovery") },
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null) },
                            label = { Text("Transcripts") },
                        )
                    }
                },
            ) { padding ->
                if (selectedTab == 0) {
                    DiscoveryScreen(
                        state = discoveryState,
                        hasMediaPermission = hasMediaPermission,
                        maxDirectUploadBytes = discoveryViewModel.maxDirectUploadBytes(),
                        onRefresh = { discoveryViewModel.refresh(hasMediaPermission) },
                        onRequestPermission = { requestPermissionLauncher.launch(requiredAudioPermission()) },
                        onChooseFolder = { folderLauncher.launch(null) },
                        onToggleSelection = discoveryViewModel::toggleSelection,
                        onTranscribeSelected = {
                            if (settingsState.isKeyValid) {
                                discoveryViewModel.transcribeSelected()
                                selectedTab = 1
                            } else {
                                showApiKeyDialog = true
                            }
                        },
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    TranscriptsScreen(
                        jobs = jobs,
                        onOpenDetail = { detailJobId = it },
                        onShare = { shareTranscript(it.notes, it.plainText) },
                        onRetry = transcriptsViewModel::retry,
                        onRetryFailed = transcriptsViewModel::retryFailed,
                        onRestartQueue = transcriptsViewModel::restartQueue,
                        onFailActiveJobs = transcriptsViewModel::markActiveJobsFailed,
                        actionMessage = transcriptActionMessage,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }

        if (showApiKeyDialog) {
            ApiKeyDialog(
                initialValue = settingsRepository.getApiKey().orEmpty(),
                validation = settingsState.keyValidation,
                onDismiss = { showApiKeyDialog = false },
                onSaveAndTest = settingsViewModel::saveAndValidateKey,
                onTestSavedKey = settingsViewModel::validateSavedKey,
            )
        }
    }

    private fun isAudioPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(this, requiredAudioPermission()) == PackageManager.PERMISSION_GRANTED

    private fun shareTranscript(notes: String?, transcript: String?) {
        val intent = Intent.createChooser(buildShareIntent(notes, transcript), "Share transcript")
        startActivity(intent)
    }
}

@Composable
private fun ApiKeyStatusIcon(validation: ApiKeyValidationSnapshot) {
    if (validation.status == ApiKeyValidationStatus.CHECKING) {
        CircularProgressIndicator(modifier = Modifier.padding(8.dp), strokeWidth = 2.dp)
        return
    }

    val tint = when (validation.status) {
        ApiKeyValidationStatus.VALID -> Color(0xFF15803D)
        ApiKeyValidationStatus.INVALID -> MaterialTheme.colorScheme.error
        ApiKeyValidationStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
        ApiKeyValidationStatus.CHECKING -> MaterialTheme.colorScheme.primary
    }
    val description = when (validation.status) {
        ApiKeyValidationStatus.VALID -> "API key valid"
        ApiKeyValidationStatus.INVALID -> "API key invalid"
        ApiKeyValidationStatus.UNKNOWN -> "API key not tested"
        ApiKeyValidationStatus.CHECKING -> "Testing API key"
    }
    Icon(
        imageVector = Icons.Default.Key,
        contentDescription = description,
        tint = tint,
    )
}

private class SimpleViewModelFactory<T : ViewModel>(
    private val createViewModel: () -> T,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = createViewModel() as T
}
