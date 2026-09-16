package com.ailivebear.app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings(
            tiktokUsername = "",
            aiProvider = AppSettings.DEFAULT_AI_PROVIDER,
            aiApiKey = "",
            characterId = AppSettings.DEFAULT_CHARACTER_ID,
            backgroundId = AppSettings.DEFAULT_BACKGROUND_ID
        )
    )

    fun onTikTokUsernameChanged(value: String) = viewModelScope.launch { repository.setTikTokUsername(value) }
    fun onAiProviderChanged(value: String) = viewModelScope.launch { repository.setAiProvider(value) }
    fun onAiApiKeyChanged(value: String) = viewModelScope.launch { repository.setAiApiKey(value) }
    fun onCharacterChanged(value: String) = viewModelScope.launch { repository.setCharacterId(value) }
    fun onBackgroundChanged(value: String) = viewModelScope.launch { repository.setBackgroundId(value) }
}

class SettingsViewModelFactory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(repository) as T
    }
}
