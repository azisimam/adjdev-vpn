package id.adjdev.vpn.vpn

import android.content.Context
import android.util.Log
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.BufferedReader
import java.io.StringReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Titik integrasi tunggal dengan backend WireGuard resmi (com.wireguard.android:tunnel).
 * Backend ini menjalankan implementasi WireGuard userspace (wireguard-go) yang sesungguhnya
 * di dalam VpnService Android - BUKAN simulasi, proxy palsu, atau WebView.
 *
 * Referensi resmi: https://github.com/WireGuard/wireguard-android
 */
object TunnelManager {

    private const val TAG = "TunnelManager"

    private var backend: Backend? = null
    private var activeTunnel: NamedTunnel? = null

    private val _state = MutableStateFlow(ConnectionState())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (backend == null) {
            backend = GoBackend(context.applicationContext)
        }
    }

    /**
     * Memulai tunnel. Pemanggil WAJIB sudah memastikan VpnService.prepare(context) == null
     * (izin VPN sudah disetujui pengguna) sebelum memanggil fungsi ini.
     */
    @Synchronized
    fun connect(
        context: Context,
        profileName: String,
        rawWgQuickConfig: String
    ): Result<Unit> {
        ensureInitialized(context)
        val backendRef = backend ?: return Result.failure(IllegalStateException("Backend belum siap"))

        return try {
            val config: Config = Config.parse(BufferedReader(StringReader(rawWgQuickConfig)))
            val endpoint = config.peers.firstOrNull()?.endpoint?.orElse(null)?.toString()
            val clientAddress = config.`interface`.addresses.joinToString(", ")

            _state.value = ConnectionState(
                status = ConnectionStatus.CONNECTING,
                activeProfileName = profileName,
                endpoint = endpoint,
                clientAddress = clientAddress
            )

            val tunnel = NamedTunnel(profileName) { newState ->
                onTunnelStateChanged(newState)
            }
            activeTunnel = tunnel

            backendRef.setState(tunnel, Tunnel.State.UP, config)

            _state.value = _state.value.copy(
                status = ConnectionStatus.CONNECTED,
                connectedSinceEpochMillis = System.currentTimeMillis(),
                errorMessage = null
            )
            Result.success(Unit)
        } catch (e: BackendException) {
            Log.w(TAG, "Gagal memulai tunnel: ${e.reason}")
            _state.value = _state.value.copy(
                status = ConnectionStatus.FAILED,
                errorMessage = mapBackendError(e)
            )
            Result.failure(e)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal memulai tunnel: ${e.javaClass.simpleName}")
            _state.value = _state.value.copy(
                status = ConnectionStatus.FAILED,
                errorMessage = "Interface WireGuard gagal dibuat."
            )
            Result.failure(e)
        }
    }

    @Synchronized
    fun disconnect() {
        val backendRef = backend
        val tunnel = activeTunnel
        if (backendRef != null && tunnel != null) {
            try {
                backendRef.setState(tunnel, Tunnel.State.DOWN, null)
            } catch (e: Exception) {
                Log.w(TAG, "Gagal menghentikan tunnel: ${e.javaClass.simpleName}")
            }
        }
        activeTunnel = null
        _state.value = ConnectionState(status = ConnectionStatus.DISCONNECTED)
    }

    /** Dipanggil secara berkala oleh service untuk memperbarui statistik (bytes, handshake). */
    fun refreshStatistics() {
        val backendRef = backend
        val tunnel = activeTunnel
        if (backendRef == null || tunnel == null) return
        if (_state.value.status != ConnectionStatus.CONNECTED) return
        try {
            val stats = backendRef.getStatistics(tunnel)
            var rx = 0L
            var tx = 0L
            var lastHandshake: Long? = null
            for (peerKey in stats.peers()) {
                val peerStats = stats.peer(peerKey) ?: continue
                rx += peerStats.rxBytes
                tx += peerStats.txBytes
                val handshake = peerStats.latestHandshakeEpochMillis
                if (handshake > 0 && (lastHandshake == null || handshake > lastHandshake!!)) {
                    lastHandshake = handshake
                }
            }
            _state.value = _state.value.copy(
                bytesReceived = rx,
                bytesSent = tx,
                lastHandshakeEpochMillis = lastHandshake
            )
        } catch (e: Exception) {
            Log.w(TAG, "Gagal membaca statistik tunnel: ${e.javaClass.simpleName}")
        }
    }

    fun isTunnelActive(): Boolean = activeTunnel != null

    private fun onTunnelStateChanged(newState: Tunnel.State) {
        when (newState) {
            Tunnel.State.UP -> {
                if (_state.value.status != ConnectionStatus.CONNECTED) {
                    _state.value = _state.value.copy(
                        status = ConnectionStatus.CONNECTED,
                        connectedSinceEpochMillis = System.currentTimeMillis()
                    )
                }
            }
            Tunnel.State.DOWN -> {
                _state.value = ConnectionState(status = ConnectionStatus.DISCONNECTED)
            }
            else -> Unit
        }
    }

    private fun mapBackendError(e: BackendException): String {
        // BackendException.Reason mencakup kasus seperti izin VPN ditolak,
        // konfigurasi tidak valid, atau interface gagal dibuat.
        return "Interface WireGuard gagal dibuat: ${e.reason}"
    }

    private class NamedTunnel(
        private val tunnelName: String,
        private val onChange: (Tunnel.State) -> Unit
    ) : Tunnel {
        override fun getName(): String = tunnelName
        override fun onStateChange(newState: Tunnel.State) = onChange(newState)
    }
}
