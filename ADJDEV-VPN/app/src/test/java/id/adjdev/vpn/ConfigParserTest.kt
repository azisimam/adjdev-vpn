package id.adjdev.vpn

import id.adjdev.vpn.data.ConfigParseErrorId
import id.adjdev.vpn.data.ConfigParseResult
import id.adjdev.vpn.data.ConfigParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigParserTest {

    // Kunci base64 44-karakter yang valid secara FORMAT (32 byte acak),
    // hanya untuk keperluan unit test - bukan kunci nyata.
    private val validPrivateKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    private val validPublicKey = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBA="

    private fun validConfigText(
        privateKey: String = validPrivateKey,
        publicKey: String = validPublicKey,
        endpoint: String = "148.230.96.102:51820"
    ) = """
        # Contoh konfigurasi
        [Interface]
        PrivateKey = $privateKey
        Address = 10.8.0.2/32
        DNS = 1.1.1.1

        [Peer]
        PublicKey = $publicKey
        Endpoint = $endpoint
        AllowedIPs = 0.0.0.0/0
        PersistentKeepalive = 25
    """.trimIndent()

    @Test
    fun `parses a valid config successfully`() {
        val result = ConfigParser.parse(validConfigText())
        assertTrue(result is ConfigParseResult.Success)
        val success = result as ConfigParseResult.Success
        assertEquals("10.8.0.2/32", success.config.interfaceSection.address)
        assertEquals("148.230.96.102", success.config.peerSection.endpointHost)
        assertEquals(51820, success.config.peerSection.endpointPort)
        assertEquals(25, success.config.peerSection.persistentKeepalive)
    }

    @Test
    fun `ignores comments starting with hash or semicolon`() {
        val text = """
            ; komentar gaya semicolon
            [Interface]
            # komentar gaya hash
            PrivateKey = $validPrivateKey
            Address = 10.8.0.2/32

            [Peer]
            PublicKey = $validPublicKey
            Endpoint = 148.230.96.102:51820
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        val result = ConfigParser.parse(text)
        assertTrue(result is ConfigParseResult.Success)
    }

    @Test
    fun `empty file returns EMPTY_FILE error`() {
        val result = ConfigParser.parse("   ")
        assertEquals(ConfigParseErrorId.EMPTY_FILE, (result as ConfigParseResult.Error).messageId)
    }

    @Test
    fun `missing Interface section is rejected`() {
        val text = """
            [Peer]
            PublicKey = $validPublicKey
            Endpoint = 148.230.96.102:51820
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        val result = ConfigParser.parse(text)
        assertEquals(ConfigParseErrorId.MISSING_INTERFACE_SECTION, (result as ConfigParseResult.Error).messageId)
    }

    @Test
    fun `missing Peer section is rejected`() {
        val text = """
            [Interface]
            PrivateKey = $validPrivateKey
            Address = 10.8.0.2/32
        """.trimIndent()
        val result = ConfigParser.parse(text)
        assertEquals(ConfigParseErrorId.MISSING_PEER_SECTION, (result as ConfigParseResult.Error).messageId)
    }

    @Test
    fun `missing private key is rejected`() {
        val text = """
            [Interface]
            Address = 10.8.0.2/32

            [Peer]
            PublicKey = $validPublicKey
            Endpoint = 148.230.96.102:51820
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        val result = ConfigParser.parse(text)
        assertEquals(ConfigParseErrorId.MISSING_PRIVATE_KEY, (result as ConfigParseResult.Error).messageId)
    }

    @Test
    fun `missing server public key is rejected`() {
        val text = """
            [Interface]
            PrivateKey = $validPrivateKey
            Address = 10.8.0.2/32

            [Peer]
            Endpoint = 148.230.96.102:51820
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        val result = ConfigParser.parse(text)
        assertEquals(ConfigParseErrorId.MISSING_SERVER_PUBLIC_KEY, (result as ConfigParseResult.Error).messageId)
    }

    @Test
    fun `malformed key length is rejected`() {
        val result = ConfigParser.parse(validConfigText(privateKey = "tooShortKey="))
        assertEquals(ConfigParseErrorId.INVALID_PRIVATE_KEY, (result as ConfigParseResult.Error).messageId)
    }

    @Test
    fun `invalid endpoint without port is rejected`() {
        val result = ConfigParser.parse(validConfigText(endpoint = "148.230.96.102"))
        assertEquals(ConfigParseErrorId.INVALID_ENDPOINT, (result as ConfigParseResult.Error).messageId)
    }

    @Test
    fun `invalid port number is rejected`() {
        val result = ConfigParser.parse(validConfigText(endpoint = "148.230.96.102:999999"))
        assertEquals(ConfigParseErrorId.INVALID_PORT, (result as ConfigParseResult.Error).messageId)
    }

    @Test
    fun `non numeric port is rejected`() {
        val result = ConfigParser.parse(validConfigText(endpoint = "148.230.96.102:abc"))
        assertEquals(ConfigParseErrorId.INVALID_PORT, (result as ConfigParseResult.Error).messageId)
    }

    @Test
    fun `all error messages are non blank Indonesian text`() {
        ConfigParseErrorId.values().forEach {
            assertTrue(it.messageIndonesian().isNotBlank())
        }
    }
}
