package com.ailivebear.app

import android.app.Application
import com.ailivebear.app.settings.SettingsRepository

/**
 * Simple manual service locator - no DI framework for Stage 1's small
 * surface area. Owns the one app-wide singleton so far (SettingsRepository,
 * since DataStore should not be re-instantiated per screen). Later stages
 * can add AIProvider/TTSProvider/TikTokCommentProvider instances here too,
 * or swap this for Hilt/Koin if the app grows enough to warrant it -
 * nothing outside this class depends on it being a manual locator.
 */
class AILiveBearApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
    }
}
