package id.adjdev.vpn.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import id.adjdev.vpn.R
import id.adjdev.vpn.data.ConfigRepository
import id.adjdev.vpn.data.TunnelProfile

@Composable
fun ConfigListScreen(
    configRepository: ConfigRepository,
    onBack: () -> Unit
) {
    var profiles by remember { mutableStateOf(configRepository.listProfiles()) }
    var activeId by remember { mutableStateOf(configRepository.getActiveProfileId()) }
    var renamingProfile by remember { mutableStateOf<TunnelProfile?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deletingProfile by remember { mutableStateOf<TunnelProfile?>(null) }

    fun refresh() {
        profiles = configRepository.listProfiles()
        activeId = configRepository.getActiveProfileId()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResourceCompat(R.string.config_list_title)) }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            items(profiles) { profile ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(profile.name + if (profile.id == activeId) "  •  aktif" else "")
                        Text(profile.endpoint)
                        Row {
                            TextButton(onClick = {
                                renamingProfile = profile
                                renameText = profile.name
                            }) { Text(stringResourceCompat(R.string.action_rename)) }

                            TextButton(onClick = {
                                configRepository.setActiveProfileId(profile.id)
                                refresh()
                            }) { Text(stringResourceCompat(R.string.action_activate)) }

                            TextButton(onClick = { deletingProfile = profile }) {
                                Text(stringResourceCompat(R.string.action_delete))
                            }
                        }
                    }
                }
            }
        }
    }

    renamingProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = { renamingProfile = null },
            title = { Text(stringResourceCompat(R.string.action_rename)) },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it })
            },
            confirmButton = {
                TextButton(onClick = {
                    configRepository.rename(profile.id, renameText.ifBlank { profile.name })
                    renamingProfile = null
                    refresh()
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { renamingProfile = null }) { Text("Batal") }
            }
        )
    }

    deletingProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = { deletingProfile = null },
            title = { Text(stringResourceCompat(R.string.confirm_delete_title)) },
            text = { Text(stringResourceCompat(R.string.confirm_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    configRepository.delete(profile.id)
                    deletingProfile = null
                    refresh()
                }) { Text(stringResourceCompat(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingProfile = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun stringResourceCompat(id: Int) = androidx.compose.ui.res.stringResource(id)
