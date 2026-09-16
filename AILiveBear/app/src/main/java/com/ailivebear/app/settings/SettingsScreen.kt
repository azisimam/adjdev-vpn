package com.ailivebear.app.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val AVAILABLE_CHARACTERS = listOf("bubu_bear" to "Bubu (Bear)")
private val AVAILABLE_BACKGROUNDS = listOf(
    "studio_soft" to "Soft Studio",
    "solid_dark" to "Solid Dark",
    "solid_light" to "Solid Light"
)
private val AVAILABLE_AI_PROVIDERS = listOf(
    "none" to "Not configured",
    "openai" to "OpenAI (wired up in Stage 4)",
    "custom" to "Custom endpoint (wired up in Stage 4)"
)

/**
 * Settings screen (architecture doc: TikTok username, AI provider/API key,
 * character selection, background selection - all persisted locally).
 * Reached only by swiping from the character screen (see ui/RootScreen.kt);
 * this composable itself never decides navigation beyond the back arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(repository))
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to character")
            }
            Spacer(Modifier.width(4.dp))
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(32.dp))

        SettingsSection(title = "TikTok LIVE") {
            OutlinedTextField(
                value = settings.tiktokUsername,
                onValueChange = viewModel::onTikTokUsernameChanged,
                label = { Text("TikTok username (e.g. @username)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Connected in a later stage. Comments will be read from this account's LIVE.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(28.dp))

        SettingsSection(title = "AI Provider") {
            ChoiceRow(
                options = AVAILABLE_AI_PROVIDERS,
                selectedId = settings.aiProvider,
                onSelected = viewModel::onAiProviderChanged
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = settings.aiApiKey,
                onValueChange = viewModel::onAiApiKeyChanged,
                label = { Text("API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Not used yet. AIProvider wiring arrives in Stage 4.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(28.dp))

        SettingsSection(title = "Character") {
            ChoiceRow(
                options = AVAILABLE_CHARACTERS,
                selectedId = settings.characterId,
                onSelected = viewModel::onCharacterChanged
            )
            Text(
                "More characters can be added later without changing this screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(28.dp))

        SettingsSection(title = "Background") {
            ChoiceRow(
                options = AVAILABLE_BACKGROUNDS,
                selectedId = settings.backgroundId,
                onSelected = viewModel::onBackgroundChanged
            )
        }

        Spacer(Modifier.height(40.dp))
        Text(
            "Swipe from left to right to return to the character.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun ChoiceRow(
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelected: (String) -> Unit
) {
    Column {
        options.forEach { (id, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(selected = selectedId == id, onClick = { onSelected(id) })
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
