package id.adjdev.vpn.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.adjdev.vpn.R
import id.adjdev.vpn.data.ConfigParseResult
import id.adjdev.vpn.data.ConfigParser
import id.adjdev.vpn.data.ConfigRepository
import id.adjdev.vpn.ui.theme.StatusFailed
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun AddConfigScreen(
    onNavigateManual: () -> Unit,
    onNavigateQr: () -> Unit,
    onImported: () -> Unit,
    configRepository: ConfigRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            errorMessage = context.getString(R.string.error_import_cancelled)
            return@rememberLauncherForActivityResult
        }
        val text = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }
        } catch (e: Exception) {
            null
        }
        if (text == null) {
            errorMessage = context.getString(R.string.error_parse_generic)
            return@rememberLauncherForActivityResult
        }
        when (val result = ConfigParser.parse(text)) {
            is ConfigParseResult.Success -> {
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Konfigurasi Import"
                configRepository.saveNewProfile(name, result.config)
                onImported()
            }
            is ConfigParseResult.Error -> {
                errorMessage = result.messageId.messageIndonesian()
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResourceCompat(R.string.menu_add_config)) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text(stringResourceCompat(R.string.menu_import_file)) }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateQr,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text(stringResourceCompat(R.string.menu_scan_qr)) }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateManual,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text(stringResourceCompat(R.string.menu_manual)) }

            errorMessage?.let {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                Text(text = it, color = StatusFailed)
            }
        }
    }
}

@Composable
private fun stringResourceCompat(id: Int) = androidx.compose.ui.res.stringResource(id)
