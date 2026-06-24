package com.techtower.meetingtranscriber.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techtower.meetingtranscriber.settings.SettingsRepository
import com.techtower.meetingtranscriber.transcription.TranscriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoveryUiState(
    val files: List<DiscoveredAudioFile> = emptyList(),
    val transcribedAudioUris: Set<String> = emptySet(),
    val selectedIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isCompactList: Boolean = false,
    val isRefreshing: Boolean = false,
    val message: String? = null,
) {
    val sections: DiscoveryListSections<DiscoveredAudioFile> =
        buildDiscoveryListSections(
            files = files,
            searchQuery = searchQuery,
            transcribedUris = transcribedAudioUris,
            displayName = { it.displayName },
            audioUri = { it.uri.toString() },
            isPriority = { isPriorityRecording(it.sizeBytes, it.durationMillis) },
        )
    val priorityFiles: List<DiscoveredAudioFile> = sections.priorityFiles
    val browseFiles: List<DiscoveredAudioFile> = sections.browseFiles
    val transcribedFiles: List<DiscoveredAudioFile> = sections.transcribedFiles
    val selectedFiles: List<DiscoveredAudioFile> = browseFiles.filter { it.id in selectedIds }
    val selectedFileCount: Int = selectedFiles.size
}

class DiscoveryViewModel(
    private val discoveryRepository: AudioDiscoveryRepository,
    private val transcriptRepository: TranscriptRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        DiscoveryUiState(isCompactList = settingsRepository.isDiscoveryCompactList()),
    )
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            transcriptRepository.observeTranscribedAudioUris().collect { transcribedUris ->
                _uiState.update { state ->
                    state.copy(
                        transcribedAudioUris = transcribedUris,
                        selectedIds = state.selectedIds.intersect(state.selectableFileIds(transcribedUris)),
                    )
                }
            }
        }
    }

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
                            selectedIds = it.selectedIds.intersect(
                                files.filterNot { file -> file.uri.toString() in it.transcribedAudioUris }
                                    .map { file -> file.id }
                                    .toSet(),
                            ),
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

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleCompactList() {
        val enabled = !_uiState.value.isCompactList
        settingsRepository.saveDiscoveryCompactList(enabled)
        _uiState.update { it.copy(isCompactList = enabled) }
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

    private fun DiscoveryUiState.selectableFileIds(transcribedUris: Set<String>): Set<String> =
        files.filterNot { it.uri.toString() in transcribedUris }.map { it.id }.toSet()
}
