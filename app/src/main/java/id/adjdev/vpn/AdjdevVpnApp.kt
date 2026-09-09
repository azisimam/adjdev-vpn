package id.adjdev.vpn

import android.app.Application
import id.adjdev.vpn.vpn.TunnelManager

class AdjdevVpnApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TunnelManager.ensureInitialized(this)
    }
}
