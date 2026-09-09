package id.adjdev.vpn.data

/**
 * Metadata konfigurasi tersimpan. Tidak berisi private key -
 * isi konfigurasi mentah (termasuk private key) disimpan terpisah
 * di penyimpanan terenkripsi melalui ConfigRepository.
 */
data class TunnelProfile(
    val id: String,
    val name: String,
    val endpoint: String,
    val clientAddress: String,
    val createdAtEpochMillis: Long
)
