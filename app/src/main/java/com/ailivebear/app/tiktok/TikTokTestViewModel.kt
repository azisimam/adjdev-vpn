package com.ailivebear.app.tiktok

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs the "Test connection" panel in Settings (see SettingsScreen.kt).
 * Lets you confirm, on the device itself, that
 * [WebSocketTikTokCommentProvider] actually receives comments from a real
 * `wss://ws.adjdev.site` session - independent of whether anything in the
 * app reacts to them yet. CharacterManager does not depend on this
 * ViewModel; a later stage wires WebSocketTikTokCommentProvider into
 * CharacterManager/AnimationController directly instead of through here.
 */
class TikTokTestViewModel : ViewModel() {

    private val provider = WebSocketTikTokCommentProvider()

    private val _uiState = MutableStateFlow(TikTokTestUiState())
    val uiState: StateFlow<TikTokTestUiState> = _uiState

    init {
        provider.onStatusChanged { status ->
            _uiState.update { it.copy(status = status) }
        }
        provider.onComment { comment ->
            _uiState.update { state ->
                state.copy(recentComments = (listOf(comment) + state.recentComments).take(6))
            }
        }
        // onGift / onFollow intentionally not surfaced in this test panel yet -
        // their payload shape isn't confirmed against the relay (see
        // WebSocketTikTokCommentProvider's kdoc). Wire them in here the same
        // way once verified.
    }

    fun connect(username: String) {
        if (username.isBlank()) return
        viewModelScope.launch { provider.connect(username) }
    }

    fun disconnect() {
        viewModelScope.launch { provider.disconnect() }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { provider.disconnect() }
    }
}

data class TikTokTestUiState(
    val status: TikTokConnectionStatus = TikTokConnectionStatus.DISCONNECTED,
    val recentComments: List<Comment> = emptyList()
)

class TikTokTestViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TikTokTestViewModel() as T
    }
}
