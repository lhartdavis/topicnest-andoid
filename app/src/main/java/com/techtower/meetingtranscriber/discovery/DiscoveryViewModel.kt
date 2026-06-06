package com.techtower.meetingtranscriber.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techtower.meetingtranscriber.settings.SettingsRepository
import com.techtower.meetingtranscriber.transcription.TranscriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoveryUiState(
    val files: List<DiscoveredAudioFile> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isRefreshing: Boolean = false,
    val message: String? = null,
) {
    val priorityFiles: List<DiscoveredAudioFile> = priorityRecordings(files)
    val selectedFiles: List<DiscoveredAudioFile> = files.filter { it.id in selectedIds }
}

class DiscoveryViewModel(
    private val discoveryRepository: AudioDiscoveryRepository,
    private val transcriptRepository: TranscriptRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    fun refresh(hasMediaPermission: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, message = null) }
            runCatching { discoveryRepository.discover(hasMediaPermission) }
                .onSuccess { files ->
                    val message = if (files.isEmpty()) {
                        "No audio files found in Music/Record/SoundRecord. Tap Refresh or choose the folder manually."
                    } else {
                        null
                    }
                    _uiState.update {
                        it.copy(
                            files = files,
                            selectedIds = it.selectedIds.intersect(files.map { file -> file.id }.toSet()),
                            isRefreshing = false,
                            message = message,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isRefreshing = false, message = error.message ?: "Discovery failed.")
                    }
                }
        }
    }

    fun toggleSelection(fileId: String) {
        _uiState.update { state ->
            val selected = if (fileId in state.selectedIds) {
                state.selectedIds - fileId
            } else {
                state.selectedIds + fileId
            }
            state.copy(selectedIds = selected)
        }
    }

    fun hasApiKey(): Boolean = settingsRepository.hasApiKey()

    fun maxDirectUploadBytes(): Long = settingsRepository.getMaxDirectUploadBytes()

    fun transcribeSelected() {
        val files = _uiState.value.selectedFiles
        if (files.isEmpty()) return
        viewModelScope.launch {
            transcriptRepository.enqueueTranscriptionJobs(files)
            _uiState.update { it.copy(selectedIds = emptySet(), message = "Queued ${files.size} transcription job(s).") }
        }
    }
}
