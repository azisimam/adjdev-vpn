package id.adjdev.vpn.vpn

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED
}

data class ConnectionState(
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val activeProfileName: String? = null,
    val endpoint: String? = null,
    val clientAddress: String? = null,
    val connectedSinceEpochMillis: Long? = null,
    val bytesReceived: Long = 0L,
    val bytesSent: Long = 0L,
    val lastHandshakeEpochMillis: Long? = null,
    val errorMessage: String? = null
)
