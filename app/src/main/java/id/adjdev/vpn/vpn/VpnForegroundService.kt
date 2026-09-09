package id.adjdev.vpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import id.adjdev.vpn.MainActivity
import id.adjdev.vpn.R
import id.adjdev.vpn.data.ConfigRepository

/**
 * Foreground service yang menampilkan notifikasi status VPN dan menyediakan
 * tombol Disconnect langsung dari notifikasi, sesuai persyaratan Android
 * untuk operasi VpnService yang berjalan lama.
 */
class VpnForegroundService : Service() {

    private var statsTicker: CountDownTimer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
                val profileName = intent.getStringExtra(EXTRA_PROFILE_NAME)
                if (profileId != null && profileName != null) {
                    startForeground(NOTIFICATION_ID, buildNotification(connecting = true))
                    performConnect(profileId, profileName)
                }
            }
            ACTION_DISCONNECT -> {
                performDisconnect()
            }
        }
        return START_NOT_STICKY
    }

    private fun performConnect(profileId: String, profileName: String) {
        val repository = ConfigRepository(applicationContext)
        val rawConfig = repository.getRawConfig(profileId)
        if (rawConfig == null) {
            stopForegroundCompat()
            stopSelf()
            return
        }
        TunnelManager.ensureInitialized(applicationContext)
        val result = TunnelManager.connect(applicationContext, profileName, rawConfig)
        if (result.isSuccess) {
            repository.setActiveProfileId(profileId)
            startForeground(NOTIFICATION_ID, buildNotification(connecting = false))
            startStatsTicker()
        } else {
            stopForegroundCompat()
            stopSelf()
        }
    }

    private fun performDisconnect() {
        statsTicker?.cancel()
        TunnelManager.disconnect()
        stopForegroundCompat()
        stopSelf()
    }

    private fun startStatsTicker() {
        statsTicker?.cancel()
        // CountDownTimer diberi durasi panjang dan diulang manual agar sederhana
        // tanpa menambah dependency scheduler tambahan.
        statsTicker = object : CountDownTimer(Long.MAX_VALUE, 3000L) {
            override fun onTick(millisUntilFinished: Long) {
                TunnelManager.refreshStatistics()
            }
            override fun onFinish() = Unit
        }.also { it.start() }
    }

    private fun buildNotification(connecting: Boolean): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val disconnectIntent = PendingIntent.getService(
            this, 0,
            Intent(this, VpnForegroundService::class.java).setAction(ACTION_DISCONNECT),
            PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (connecting) getString(R.string.notif_title_connecting) else getString(R.string.notif_title_connected)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.notif_action_disconnect), disconnectIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        statsTicker?.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "id.adjdev.vpn.action.CONNECT"
        const val ACTION_DISCONNECT = "id.adjdev.vpn.action.DISCONNECT"
        const val EXTRA_PROFILE_ID = "extra_profile_id"
        const val EXTRA_PROFILE_NAME = "extra_profile_name"
        private const val CHANNEL_ID = "adjdev_vpn_status"
        private const val NOTIFICATION_ID = 1001
    }
}
