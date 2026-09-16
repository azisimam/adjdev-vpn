package com.ailivebear.app.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ailivebear_settings")

/**
 * Local persistence for user-configurable settings (TikTok username, AI
 * provider/key, character choice, background choice).
 *
 * Stage 1 persists everything via Jetpack DataStore Preferences, which is
 * NOT encrypted at rest. That's fine while the API key field is empty and
 * unused. Before Stage 4 ships (when a real AI API key is entered and
 * actually used), swap the storage backend for something Keystore-backed
 * (e.g. androidx.security's EncryptedFile) for aiApiKey specifically. The
 * public surface here (the Flow + suspend setters) is written so that swap
 * doesn't require touching SettingsViewModel or SettingsScreen.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val TIKTOK_USERNAME = stringPreferencesKey("tiktok_username")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val CHARACTER_ID = stringPreferencesKey("character_id")
        val BACKGROUND_ID = stringPreferencesKey("background_id")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            tiktokUsername = prefs[Keys.TIKTOK_USERNAME] ?: "",
            aiProvider = prefs[Keys.AI_PROVIDER] ?: AppSettings.DEFAULT_AI_PROVIDER,
            aiApiKey = prefs[Keys.AI_API_KEY] ?: "",
            characterId = prefs[Keys.CHARACTER_ID] ?: AppSettings.DEFAULT_CHARACTER_ID,
            backgroundId = prefs[Keys.BACKGROUND_ID] ?: AppSettings.DEFAULT_BACKGROUND_ID
        )
    }

    suspend fun setTikTokUsername(value: String) {
        context.dataStore.edit { it[Keys.TIKTOK_USERNAME] = value }
    }

    suspend fun setAiProvider(value: String) {
        context.dataStore.edit { it[Keys.AI_PROVIDER] = value }
    }

    suspend fun setAiApiKey(value: String) {
        context.dataStore.edit { it[Keys.AI_API_KEY] = value }
    }

    suspend fun setCharacterId(value: String) {
        context.dataStore.edit { it[Keys.CHARACTER_ID] = value }
    }

    suspend fun setBackgroundId(value: String) {
        context.dataStore.edit { it[Keys.BACKGROUND_ID] = value }
    }
}

data class AppSettings(
    val tiktokUsername: String,
    val aiProvider: String,
    val aiApiKey: String,
    val characterId: String,
    val backgroundId: String
) {
    companion object {
        const val DEFAULT_AI_PROVIDER = "none"
        const val DEFAULT_CHARACTER_ID = "bubu_bear"
        const val DEFAULT_BACKGROUND_ID = "studio_soft"
    }
}
