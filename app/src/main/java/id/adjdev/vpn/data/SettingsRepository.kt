package id.adjdev.vpn.data

import android.content.Context

/**
 * Pengaturan aplikasi yang tidak sensitif (bukan private key / secret),
 * disimpan di SharedPreferences biasa.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("adjdev_settings", Context.MODE_PRIVATE)

    var autoConnectOnBoot: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, false) // default nonaktif
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT, value).apply()

    var killSwitchEnabled: Boolean
        get() = prefs.getBoolean(KEY_KILL_SWITCH, false) // default nonaktif
        set(value) = prefs.edit().putBoolean(KEY_KILL_SWITCH, value).apply()

    var alwaysOnVpn: Boolean
        get() = prefs.getBoolean(KEY_ALWAYS_ON, false)
        set(value) = prefs.edit().putBoolean(KEY_ALWAYS_ON, value).apply()

    var customDns: String
        get() = prefs.getString(KEY_DNS, "1.1.1.1") ?: "1.1.1.1"
        set(value) = prefs.edit().putString(KEY_DNS, value).apply()

    /** Ditandai true hanya setelah pengguna berhasil melewati dialog izin VPN Android. */
    var vpnPermissionGrantedOnce: Boolean
        get() = prefs.getBoolean(KEY_VPN_PERMISSION_ONCE, false)
        set(value) = prefs.edit().putBoolean(KEY_VPN_PERMISSION_ONCE, value).apply()

    companion object {
        private const val KEY_AUTO_CONNECT = "auto_connect_on_boot"
        private const val KEY_KILL_SWITCH = "kill_switch_enabled"
        private const val KEY_ALWAYS_ON = "always_on_vpn"
        private const val KEY_DNS = "custom_dns"
        private const val KEY_VPN_PERMISSION_ONCE = "vpn_permission_granted_once"
    }
}
