package id.adjdev.vpn.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Menyimpan konfigurasi WireGuard secara lokal.
 *
 * - Isi mentah .conf (termasuk private key) disimpan HANYA di
 *   EncryptedSharedPreferences yang dibungkus oleh Android Keystore.
 * - Metadata non-sensitif (nama, endpoint, alamat client, urutan) disimpan
 *   di SharedPreferences biasa agar daftar bisa ditampilkan tanpa membuka
 *   penyimpanan terenkripsi berulang kali.
 *
 * Tidak ada bagian dari kelas ini yang mengirim data ke jaringan.
 */
class ConfigRepository(context: Context) {

    private val appContext = context.applicationContext

    private val metadataPrefs: SharedPreferences =
        appContext.getSharedPreferences(METADATA_PREFS_NAME, Context.MODE_PRIVATE)

    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun listProfiles(): List<TunnelProfile> {
        val json = metadataPrefs.getString(KEY_PROFILE_LIST, null) ?: return emptyList()
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            TunnelProfile(
                id = obj.getString("id"),
                name = obj.getString("name"),
                endpoint = obj.optString("endpoint", ""),
                clientAddress = obj.optString("clientAddress", ""),
                createdAtEpochMillis = obj.optLong("createdAt", 0L)
            )
        }
    }

    fun saveNewProfile(name: String, parsed: ParsedTunnelConfig): TunnelProfile {
        val id = UUID.randomUUID().toString()
        val profile = TunnelProfile(
            id = id,
            name = name,
            endpoint = "${parsed.peerSection.endpointHost}:${parsed.peerSection.endpointPort}",
            clientAddress = parsed.interfaceSection.address,
            createdAtEpochMillis = System.currentTimeMillis()
        )
        writeSecureConfig(id, parsed.toWgQuickText())
        val updated = listProfiles() + profile
        persistProfileList(updated)
        return profile
    }

    fun getRawConfig(profileId: String): String? =
        securePrefs.getString(rawConfigKey(profileId), null)

    fun rename(profileId: String, newName: String) {
        val updated = listProfiles().map { if (it.id == profileId) it.copy(name = newName) else it }
        persistProfileList(updated)
    }

    fun delete(profileId: String) {
        securePrefs.edit().remove(rawConfigKey(profileId)).apply()
        val updated = listProfiles().filterNot { it.id == profileId }
        persistProfileList(updated)
        if (getActiveProfileId() == profileId) {
            setActiveProfileId(null)
        }
    }

    fun clearAll() {
        securePrefs.edit().clear().apply()
        metadataPrefs.edit().clear().apply()
    }

    fun getActiveProfileId(): String? = metadataPrefs.getString(KEY_ACTIVE_PROFILE, null)

    fun setActiveProfileId(profileId: String?) {
        metadataPrefs.edit().putString(KEY_ACTIVE_PROFILE, profileId).apply()
    }

    private fun writeSecureConfig(profileId: String, rawText: String) {
        securePrefs.edit().putString(rawConfigKey(profileId), rawText).apply()
    }

    private fun rawConfigKey(profileId: String) = "config_$profileId"

    private fun persistProfileList(profiles: List<TunnelProfile>) {
        val array = JSONArray()
        profiles.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("endpoint", p.endpoint)
            obj.put("clientAddress", p.clientAddress)
            obj.put("createdAt", p.createdAtEpochMillis)
            array.put(obj)
        }
        metadataPrefs.edit().putString(KEY_PROFILE_LIST, array.toString()).apply()
    }

    companion object {
        private const val METADATA_PREFS_NAME = "adjdev_config_metadata"
        private const val SECURE_PREFS_NAME = "adjdev_secure_configs"
        private const val KEY_PROFILE_LIST = "profile_list"
        private const val KEY_ACTIVE_PROFILE = "active_profile_id"
    }
}
