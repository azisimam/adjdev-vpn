package com.ailivebear.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ailivebear.app.ui.RootScreen
import com.ailivebear.app.ui.theme.AILiveBearTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AILiveBearApp

        setContent {
            AILiveBearTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootScreen(settingsRepository = app.settingsRepository)
                }
            }
        }
    }
}
