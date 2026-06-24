package com.techtower.meetingtranscriber.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techtower.meetingtranscriber.transcription.AutonomousTranscriptionScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val keyValidation: ApiKeyValidationSnapshot = ApiKeyValidationSnapshot(),
    val autonomousModeEnabled: Boolean = false,
) {
    val isKeyValid: Boolean = keyValidation.status == ApiKeyValidationStatus.VALID
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val keyClient: OpenRouterKeyClient,
    private val autonomousScheduler: AutonomousTranscriptionScheduler,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            keyValidation = settingsRepository.getApiKeyValidation(),
            autonomousModeEnabled = settingsRepository.isAutonomousModeEnabled(),
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        autonomousScheduler.sync(settingsRepository.isAutonomousModeEnabled())
    }

    fun saveAndValidateKey(apiKey: String) {
        settingsRepository.saveApiKey(apiKey)
        validateSavedKey()
    }

    fun validateSavedKey() {
        val apiKey = settingsRepository.getApiKey()
        if (apiKey == null) {
            val snapshot = ApiKeyValidationSnapshot(
                status = ApiKeyValidationStatus.INVALID,
                message = "Paste an OpenRouter API key before testing.",
                checkedAtMillis = System.currentTimeMillis(),
            )
            settingsRepository.saveApiKeyValidation(snapshot)
            _uiState.update { it.copy(keyValidation = snapshot) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    keyValidation = it.keyValidation.copy(
                        status = ApiKeyValidationStatus.CHECKING,
                        message = "Testing key with OpenRouter...",
                    ),
                )
            }
            val result = keyClient.validate(apiKey)
            val snapshot = ApiKeyValidationSnapshot(
                status = result.status,
                message = result.message,
                label = result.label,
                checkedAtMillis = System.currentTimeMillis(),
            )
            settingsRepository.saveApiKeyValidation(snapshot)
            _uiState.update { it.copy(keyValidation = snapshot) }
        }
    }

    fun setAutonomousModeEnabled(enabled: Boolean) {
        settingsRepository.saveAutonomousModeEnabled(enabled)
        autonomousScheduler.sync(enabled)
        _uiState.update { it.copy(autonomousModeEnabled = enabled) }
    }
}
