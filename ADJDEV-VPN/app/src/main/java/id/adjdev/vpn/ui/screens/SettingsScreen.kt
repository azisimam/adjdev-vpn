package id.adjdev.vpn.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.adjdev.vpn.BuildConfig
import id.adjdev.vpn.R
import id.adjdev.vpn.data.ConfigRepository
import id.adjdev.vpn.data.SettingsRepository

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    configRepository: ConfigRepository,
    onNavigatePrivacyPolicy: () -> Unit,
    onBack: () -> Unit
) {
    var autoConnect by remember { mutableStateOf(settingsRepository.autoConnectOnBoot) }
    var killSwitch by remember { mutableStateOf(settingsRepository.killSwitchEnabled) }
    var alwaysOn by remember { mutableStateOf(settingsRepository.alwaysOnVpn) }
    var dns by remember { mutableStateOf(settingsRepository.customDns) }
    var showKillSwitchWarning by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResourceCompat(R.string.settings_title)) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {

            SettingRow(stringResourceCompat(R.string.settings_autoconnect_boot), autoConnect) {
                autoConnect = it
                settingsRepository.autoConnectOnBoot = it
            }

            SettingRow(stringResourceCompat(R.string.settings_kill_switch), killSwitch) { enabling ->
                if (enabling) {
                    showKillSwitchWarning = true
                } else {
                    killSwitch = false
                    settingsRepository.killSwitchEnabled = false
                }
            }

            SettingRow(stringResourceCompat(R.string.settings_always_on), alwaysOn) {
                alwaysOn = it
                settingsRepository.alwaysOnVpn = it
            }

            Spacer()
            OutlinedTextField(
                value = dns,
                onValueChange = {
                    dns = it
                    settingsRepository.customDns = it
                },
                label = { Text(stringResourceCompat(R.string.settings_dns)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer()
            TextButton(onClick = { showClearConfirm = true }) {
                Text(stringResourceCompat(R.string.settings_clear_all))
            }
            TextButton(onClick = { showResetConfirm = true }) {
                Text(stringResourceCompat(R.string.settings_reset_app))
            }
            TextButton(onClick = onNavigatePrivacyPolicy) {
                Text(stringResourceCompat(R.string.settings_privacy_policy))
            }

            Spacer()
            Text("${stringResourceCompat(R.string.settings_version)}: ${BuildConfig.VERSION_NAME}")
            Text(stringResourceCompat(R.string.settings_about) + ": ADJDEV VPN — klien WireGuard native.")
        }
    }

    if (showKillSwitchWarning) {
        AlertDialog(
            onDismissRequest = { showKillSwitchWarning = false },
            title = { Text(stringResourceCompat(R.string.settings_kill_switch)) },
            text = { Text(stringResourceCompat(R.string.settings_kill_switch_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    killSwitch = true
                    settingsRepository.killSwitchEnabled = true
                    showKillSwitchWarning = false
                }) { Text("Aktifkan") }
            },
            dismissButton = {
                TextButton(onClick = { showKillSwitchWarning = false }) { Text("Batal") }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResourceCompat(R.string.settings_clear_all)) },
            text = { Text(stringResourceCompat(R.string.confirm_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    configRepository.clearAll()
                    showClearConfirm = false
                }) { Text(stringResourceCompat(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Batal") } }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResourceCompat(R.string.settings_reset_app)) },
            text = { Text(stringResourceCompat(R.string.confirm_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    configRepository.clearAll()
                    autoConnect = false
                    killSwitch = false
                    alwaysOn = false
                    settingsRepository.autoConnectOnBoot = false
                    settingsRepository.killSwitchEnabled = false
                    settingsRepository.alwaysOnVpn = false
                    showResetConfirm = false
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun Spacer() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun stringResourceCompat(id: Int) = androidx.compose.ui.res.stringResource(id)
