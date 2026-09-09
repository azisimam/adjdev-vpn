package id.adjdev.vpn.data

/**
 * Hasil parsing file konfigurasi WireGuard (.conf) sebelum dikonversi
 * menjadi com.wireguard.config.Config untuk dikirim ke backend tunnel.
 */
data class ParsedInterfaceSection(
    val privateKey: String,
    val address: String,
    val dns: String?
)

data class ParsedPeerSection(
    val publicKey: String,
    val endpointHost: String,
    val endpointPort: Int,
    val allowedIps: String,
    val persistentKeepalive: Int?
)

data class ParsedTunnelConfig(
    val interfaceSection: ParsedInterfaceSection,
    val peerSection: ParsedPeerSection
) {
    /** Mengembalikan representasi wg-quick agar dapat diteruskan ke com.wireguard.config.Config.parse(). */
    fun toWgQuickText(): String = buildString {
        appendLine("[Interface]")
        appendLine("PrivateKey = ${interfaceSection.privateKey}")
        appendLine("Address = ${interfaceSection.address}")
        interfaceSection.dns?.let { appendLine("DNS = $it") }
        appendLine()
        appendLine("[Peer]")
        appendLine("PublicKey = ${peerSection.publicKey}")
        appendLine("Endpoint = ${peerSection.endpointHost}:${peerSection.endpointPort}")
        appendLine("AllowedIPs = ${peerSection.allowedIps}")
        peerSection.persistentKeepalive?.let { appendLine("PersistentKeepalive = $it") }
    }
}

sealed class ConfigParseResult {
    data class Success(val config: ParsedTunnelConfig) : ConfigParseResult()
    data class Error(val messageId: ConfigParseErrorId) : ConfigParseResult()
}

/**
 * Identitas error yang stabil untuk keperluan unit test; pemetaan ke teks
 * Bahasa Indonesia yang ditampilkan ke pengguna ada di ConfigParseErrorId.messageIndonesian().
 */
enum class ConfigParseErrorId {
    EMPTY_FILE,
    MISSING_INTERFACE_SECTION,
    MISSING_PEER_SECTION,
    MISSING_PRIVATE_KEY,
    MISSING_SERVER_PUBLIC_KEY,
    INVALID_PRIVATE_KEY,
    INVALID_PUBLIC_KEY,
    MISSING_ADDRESS,
    MISSING_ENDPOINT,
    INVALID_ENDPOINT,
    INVALID_PORT,
    INVALID_ALLOWED_IPS,
    INVALID_KEEPALIVE,
    UNKNOWN_LINE;

    fun messageIndonesian(): String = when (this) {
        EMPTY_FILE -> "File konfigurasi kosong."
        MISSING_INTERFACE_SECTION -> "Bagian [Interface] tidak ditemukan."
        MISSING_PEER_SECTION -> "Bagian [Peer] tidak ditemukan."
        MISSING_PRIVATE_KEY -> "Private key belum ada. Lengkapi konfigurasi terlebih dahulu."
        MISSING_SERVER_PUBLIC_KEY -> "Public key server belum ada. Lengkapi konfigurasi terlebih dahulu."
        INVALID_PRIVATE_KEY -> "Format private key tidak valid."
        INVALID_PUBLIC_KEY -> "Format public key server tidak valid."
        MISSING_ADDRESS -> "Address client belum diisi."
        MISSING_ENDPOINT -> "Endpoint server belum diisi."
        INVALID_ENDPOINT -> "Endpoint tidak valid."
        INVALID_PORT -> "Port tidak valid."
        INVALID_ALLOWED_IPS -> "AllowedIPs tidak valid."
        INVALID_KEEPALIVE -> "Nilai PersistentKeepalive tidak valid."
        UNKNOWN_LINE -> "Konfigurasi tidak dapat dibaca. Periksa kembali format file."
    }
}
