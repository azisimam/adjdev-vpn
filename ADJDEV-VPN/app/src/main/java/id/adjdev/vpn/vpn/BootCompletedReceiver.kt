package id.adjdev.vpn.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import id.adjdev.vpn.data.ConfigRepository
import id.adjdev.vpn.data.SettingsRepository

/**
 * Menangani auto-connect setelah reboot perangkat.
 *
 * Auto-connect HANYA berjalan jika:
 * 1. Pengguna sudah mengaktifkan opsi ini secara eksplisit (default nonaktif), DAN
 * 2. Izin VPN Android sudah pernah disetujui sebelumnya (VpnService.prepare == null),
 *    sehingga tidak ada dialog izin yang perlu ditampilkan dari luar Activity.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val settings = SettingsRepository(context)
        if (!settings.autoConnectOnBoot) return
        if (!settings.vpnPermissionGrantedOnce) return
        if (VpnService.prepare(context) != null) return // izin belum/harus disetujui ulang

        val repository = ConfigRepository(context)
        val activeId = repository.getActiveProfileId() ?: return
        val profiles = repository.listProfiles()
        val activeProfile = profiles.firstOrNull { it.id == activeId } ?: return

        val serviceIntent = Intent(context, VpnForegroundService::class.java)
            .setAction(VpnForegroundService.ACTION_CONNECT)
            .putExtra(VpnForegroundService.EXTRA_PROFILE_ID, activeProfile.id)
            .putExtra(VpnForegroundService.EXTRA_PROFILE_NAME, activeProfile.name)
        context.startForegroundService(serviceIntent)
    }
}
