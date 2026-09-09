package id.adjdev.vpn.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.adjdev.vpn.R
import id.adjdev.vpn.data.ConfigParseResult
import id.adjdev.vpn.data.ConfigParser
import id.adjdev.vpn.data.ConfigRepository
import id.adjdev.vpn.ui.theme.StatusFailed

@Composable
fun ManualEntryScreen(
    configRepository: ConfigRepository,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("10.8.0.2/32") }
    var dns by remember { mutableStateOf("1.1.1.1") }
    var serverPublicKey by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("148.230.96.102:51820") }
    var allowedIps by remember { mutableStateOf("0.0.0.0/0") }
    var keepalive by remember { mutableStateOf("25") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResourceCompat(R.string.menu_manual)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama konfigurasi") }, modifier = Modifier.fillMaxWidth())
            Spacer()
            OutlinedTextField(value = privateKey, onValueChange = { privateKey = it }, label = { Text("PrivateKey (client)") }, modifier = Modifier.fillMaxWidth())
            Spacer()
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            Spacer()
            OutlinedTextField(value = dns, onValueChange = { dns = it }, label = { Text("DNS") }, modifier = Modifier.fillMaxWidth())
            Spacer()
            OutlinedTextField(value = serverPublicKey, onValueChange = { serverPublicKey = it }, label = { Text("PublicKey (server)") }, modifier = Modifier.fillMaxWidth())
            Spacer()
            OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, label = { Text("Endpoint (host:port)") }, modifier = Modifier.fillMaxWidth())
            Spacer()
            OutlinedTextField(value = allowedIps, onValueChange = { allowedIps = it }, label = { Text("AllowedIPs") }, modifier = Modifier.fillMaxWidth())
            Spacer()
            OutlinedTextField(value = keepalive, onValueChange = { keepalive = it }, label = { Text("PersistentKeepalive") }, modifier = Modifier.fillMaxWidth())
            Spacer()

            Button(
                onClick = {
                    val raw = buildString {
                        appendLine("[Interface]")
                        appendLine("PrivateKey = $privateKey")
                        appendLine("Address = $address")
                        appendLine("DNS = $dns")
                        appendLine()
                        appendLine("[Peer]")
                        appendLine("PublicKey = $serverPublicKey")
                        appendLine("Endpoint = $endpoint")
                        appendLine("AllowedIPs = $allowedIps")
                        appendLine("PersistentKeepalive = $keepalive")
                    }
                    when (val result = ConfigParser.parse(raw)) {
                        is ConfigParseResult.Success -> {
                            val profileName = name.ifBlank { "Konfigurasi Manual" }
                            configRepository.saveNewProfile(profileName, result.config)
                            onSaved()
                        }
                        is ConfigParseResult.Error -> {
                            errorMessage = result.messageId.messageIndonesian()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("Simpan") }

            errorMessage?.let {
                Spacer()
                Text(text = it, color = StatusFailed)
            }
        }
    }
}

@Composable
private fun Spacer() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun stringResourceCompat(id: Int) = androidx.compose.ui.res.stringResource(id)
