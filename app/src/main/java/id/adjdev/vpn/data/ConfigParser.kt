package id.adjdev.vpn.data

import java.util.Base64

/**
 * Parser sederhana untuk file konfigurasi WireGuard bergaya wg-quick.
 *
 * Catatan keamanan: parser ini TIDAK PERNAH menulis private key ke Logcat
 * (lihat CATATAN di bawah setiap kali nilai key diproses).
 */
object ConfigParser {

    private val SECTION_REGEX = Regex("^\\[(.+)]$")

    fun parse(rawText: String): ConfigParseResult {
        val text = rawText.trim()
        if (text.isEmpty()) {
            return ConfigParseResult.Error(ConfigParseErrorId.EMPTY_FILE)
        }

        val interfaceValues = mutableMapOf<String, String>()
        val peerValues = mutableMapOf<String, String>()
        var currentSection: String? = null
        var sawInterfaceSection = false
        var sawPeerSection = false

        for (rawLine in text.lines()) {
            val line = stripComment(rawLine).trim()
            if (line.isEmpty()) continue

            val sectionMatch = SECTION_REGEX.find(line)
            if (sectionMatch != null) {
                currentSection = sectionMatch.groupValues[1].trim().lowercase()
                when (currentSection) {
                    "interface" -> sawInterfaceSection = true
                    "peer" -> sawPeerSection = true
                }
                continue
            }

            val separatorIndex = line.indexOf('=')
            if (separatorIndex <= 0) {
                // Baris tidak dikenali; abaikan secara toleran daripada gagal total,
                // kecuali seluruh file tidak memiliki section yang valid (ditangani di bawah).
                continue
            }
            val key = line.substring(0, separatorIndex).trim().lowercase()
            val value = line.substring(separatorIndex + 1).trim()

            when (currentSection) {
                "interface" -> interfaceValues[key] = value
                "peer" -> peerValues[key] = value
                else -> Unit
            }
        }

        if (!sawInterfaceSection) {
            return ConfigParseResult.Error(ConfigParseErrorId.MISSING_INTERFACE_SECTION)
        }
        if (!sawPeerSection) {
            return ConfigParseResult.Error(ConfigParseErrorId.MISSING_PEER_SECTION)
        }

        val privateKey = interfaceValues["privatekey"]
        if (privateKey.isNullOrBlank()) {
            return ConfigParseResult.Error(ConfigParseErrorId.MISSING_PRIVATE_KEY)
        }
        if (!isValidWireGuardKey(privateKey)) {
            return ConfigParseResult.Error(ConfigParseErrorId.INVALID_PRIVATE_KEY)
        }

        val address = interfaceValues["address"]
        if (address.isNullOrBlank()) {
            return ConfigParseResult.Error(ConfigParseErrorId.MISSING_ADDRESS)
        }

        val dns = interfaceValues["dns"]?.takeIf { it.isNotBlank() }

        val publicKey = peerValues["publickey"]
        if (publicKey.isNullOrBlank()) {
            return ConfigParseResult.Error(ConfigParseErrorId.MISSING_SERVER_PUBLIC_KEY)
        }
        if (!isValidWireGuardKey(publicKey)) {
            return ConfigParseResult.Error(ConfigParseErrorId.INVALID_PUBLIC_KEY)
        }

        val endpointRaw = peerValues["endpoint"]
        if (endpointRaw.isNullOrBlank()) {
            return ConfigParseResult.Error(ConfigParseErrorId.MISSING_ENDPOINT)
        }
        val lastColon = endpointRaw.lastIndexOf(':')
        if (lastColon <= 0 || lastColon == endpointRaw.length - 1) {
            return ConfigParseResult.Error(ConfigParseErrorId.INVALID_ENDPOINT)
        }
        val host = endpointRaw.substring(0, lastColon).trim().removeSurrounding("[", "]")
        val portText = endpointRaw.substring(lastColon + 1).trim()
        val port = portText.toIntOrNull()
        if (host.isBlank()) {
            return ConfigParseResult.Error(ConfigParseErrorId.INVALID_ENDPOINT)
        }
        if (port == null || port !in 1..65535) {
            return ConfigParseResult.Error(ConfigParseErrorId.INVALID_PORT)
        }

        val allowedIps = peerValues["allowedips"]?.takeIf { it.isNotBlank() } ?: "0.0.0.0/0"
        if (!isPlausibleAllowedIps(allowedIps)) {
            return ConfigParseResult.Error(ConfigParseErrorId.INVALID_ALLOWED_IPS)
        }

        val keepaliveText = peerValues["persistentkeepalive"]
        val keepalive = if (keepaliveText.isNullOrBlank()) {
            null
        } else {
            val parsed = keepaliveText.toIntOrNull()
            if (parsed == null || parsed !in 0..65535) {
                return ConfigParseResult.Error(ConfigParseErrorId.INVALID_KEEPALIVE)
            }
            parsed
        }

        return ConfigParseResult.Success(
            ParsedTunnelConfig(
                interfaceSection = ParsedInterfaceSection(
                    privateKey = privateKey,
                    address = address,
                    dns = dns
                ),
                peerSection = ParsedPeerSection(
                    publicKey = publicKey,
                    endpointHost = host,
                    endpointPort = port,
                    allowedIps = allowedIps,
                    persistentKeepalive = keepalive
                )
            )
        )
        // CATATAN: 'privateKey' di atas tidak pernah diteruskan ke Log.* di mana pun dalam class ini.
    }

    private fun stripComment(line: String): String {
        var inSingleQuote = false
        for (i in line.indices) {
            val c = line[i]
            if (c == '\'') inSingleQuote = !inSingleQuote
            if ((c == '#' || c == ';') && !inSingleQuote) {
                return line.substring(0, i)
            }
        }
        return line
    }

    /** WireGuard key: base64 dari 32 byte -> 44 karakter, diakhiri '='. */
    private fun isValidWireGuardKey(value: String): Boolean {
        if (value.length != 44 || !value.endsWith("=")) return false
        return try {
            val decoded = Base64.getDecoder().decode(value)
            decoded.size == 32
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun isPlausibleAllowedIps(value: String): Boolean {
        return value.split(",").all { entry ->
            val trimmed = entry.trim()
            trimmed.isNotEmpty() && trimmed.contains("/")
        }
    }
}
