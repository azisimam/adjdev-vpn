package id.adjdev.vpn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.adjdev.vpn.R
import id.adjdev.vpn.data.ConfigRepository
import id.adjdev.vpn.data.TunnelProfile
import id.adjdev.vpn.ui.theme.StatusConnected
import id.adjdev.vpn.ui.theme.StatusConnecting
import id.adjdev.vpn.ui.theme.StatusDisconnected
import id.adjdev.vpn.ui.theme.StatusFailed
import id.adjdev.vpn.vpn.ConnectionStatus
import id.adjdev.vpn.vpn.TunnelManager
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeScreen(
    configRepository: ConfigRepository,
    onRequestConnect: (TunnelProfile) -> Unit,
    onRequestDisconnect: () -> Unit,
    onNavigateAddConfig: () -> Unit,
    onNavigateConfigList: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val connectionState by TunnelManager.state.collectAsState()
    var profiles by remember { mutableStateOf(configRepository.listProfiles()) }
    var selectedProfile by remember { mutableStateOf<TunnelProfile?>(null) }

    LaunchedEffect(connectionState.status) {
        profiles = configRepository.listProfiles()
        val activeId = configRepository.getActiveProfileId()
        selectedProfile = profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResourceCompat(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateConfigList) {
                        Icon(Icons.Filled.List, contentDescription = stringResourceCompat(R.string.config_list_title))
                    }
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResourceCompat(R.string.menu_settings))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusBadge(status = connectionState.status)

            Spacer(modifier = Modifier.height(24.dp))

            val profile = selectedProfile
            if (profile == null) {
                Text(stringResourceCompat(R.string.label_no_config))
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateAddConfig) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.height(0.dp))
                    Text("  " + stringResourceCompat(R.string.menu_add_config))
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow(stringResourceCompat(R.string.label_active_config), profile.name)
                        InfoRow(stringResourceCompat(R.string.label_endpoint), connectionState.endpoint ?: profile.endpoint)
                        InfoRow(stringResourceCompat(R.string.label_client_ip), connectionState.clientAddress ?: profile.clientAddress)
                        InfoRow(stringResourceCompat(R.string.label_duration), formatDuration(connectionState.connectedSinceEpochMillis))
                        InfoRow(stringResourceCompat(R.string.label_bytes_received), formatBytes(connectionState.bytesReceived))
                        InfoRow(stringResourceCompat(R.string.label_bytes_sent), formatBytes(connectionState.bytesSent))
                        InfoRow(stringResourceCompat(R.string.label_last_handshake), formatHandshake(connectionState.lastHandshakeEpochMillis))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val buttonColor = when (connectionState.status) {
                    ConnectionStatus.CONNECTED -> StatusFailed
                    ConnectionStatus.CONNECTING -> StatusConnecting
                    else -> StatusConnected
                }

                Button(
                    onClick = {
                        if (connectionState.status == ConnectionStatus.CONNECTED ||
                            connectionState.status == ConnectionStatus.CONNECTING
                        ) {
                            onRequestDisconnect()
                        } else {
                            onRequestConnect(profile)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                ) {
                    val label = if (connectionState.status == ConnectionStatus.CONNECTED ||
                        connectionState.status == ConnectionStatus.CONNECTING
                    ) stringResourceCompat(R.string.btn_disconnect) else stringResourceCompat(R.string.btn_connect)
                    Text(label, color = Color.White)
                }

                connectionState.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = message, color = StatusFailed)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onNavigateAddConfig) {
                Text(stringResourceCompat(R.string.menu_add_config))
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ConnectionStatus) {
    val (color, textRes) = when (status) {
        ConnectionStatus.CONNECTED -> StatusConnected to R.string.status_connected
        ConnectionStatus.CONNECTING -> StatusConnecting to R.string.status_connecting
        ConnectionStatus.FAILED -> StatusFailed to R.string.status_failed
        ConnectionStatus.DISCONNECTED -> StatusDisconnected to R.string.status_disconnected
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResourceCompat(textRes), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(modifier = Modifier.height(6.dp))
}

private fun formatDuration(connectedSince: Long?): String {
    if (connectedSince == null) return "-"
    val elapsedSeconds = (System.currentTimeMillis() - connectedSince) / 1000
    val h = elapsedSeconds / 3600
    val m = (elapsedSeconds % 3600) / 60
    val s = elapsedSeconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}

private fun formatHandshake(epochMillis: Long?): String {
    if (epochMillis == null || epochMillis == 0L) return "-"
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(epochMillis)
}

@Composable
private fun stringResourceCompat(id: Int) = androidx.compose.ui.res.stringResource(id)
