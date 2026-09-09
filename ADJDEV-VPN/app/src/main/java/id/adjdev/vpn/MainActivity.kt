package id.adjdev.vpn

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import id.adjdev.vpn.data.ConfigRepository
import id.adjdev.vpn.data.SettingsRepository
import id.adjdev.vpn.data.TunnelProfile
import id.adjdev.vpn.ui.screens.AddConfigScreen
import id.adjdev.vpn.ui.screens.ConfigListScreen
import id.adjdev.vpn.ui.screens.HomeScreen
import id.adjdev.vpn.ui.screens.ManualEntryScreen
import id.adjdev.vpn.ui.screens.PrivacyPolicyScreen
import id.adjdev.vpn.ui.screens.QrScanScreen
import id.adjdev.vpn.ui.screens.SettingsScreen
import id.adjdev.vpn.ui.theme.AdjdevVpnTheme
import id.adjdev.vpn.vpn.VpnForegroundService

class MainActivity : ComponentActivity() {

    private lateinit var configRepository: ConfigRepository
    private lateinit var settingsRepository: SettingsRepository

    /** Profil yang menunggu koneksi setelah dialog izin VPN Android disetujui. */
    private var pendingConnectProfile: TunnelProfile? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val profile = pendingConnectProfile
        pendingConnectProfile = null
        if (result.resultCode == RESULT_OK && profile != null) {
            settingsRepository.vpnPermissionGrantedOnce = true
            startVpnService(profile)
        }
        // Jika ditolak, layar Home akan tetap menampilkan status Terputus / pesan error,
        // sesuai persyaratan: jangan pernah menampilkan status Terhubung tanpa izin.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configRepository = ConfigRepository(applicationContext)
        settingsRepository = SettingsRepository(applicationContext)

        setContent {
            AdjdevVpnTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    configRepository = configRepository,
                    settingsRepository = settingsRepository,
                    onRequestConnect = ::requestConnect,
                    onRequestDisconnect = ::requestDisconnect
                )
            }
        }
    }

    private fun requestConnect(profile: TunnelProfile) {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            pendingConnectProfile = profile
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            settingsRepository.vpnPermissionGrantedOnce = true
            startVpnService(profile)
        }
    }

    private fun requestDisconnect() {
        val intent = Intent(this, VpnForegroundService::class.java)
            .setAction(VpnForegroundService.ACTION_DISCONNECT)
        startService(intent)
    }

    private fun startVpnService(profile: TunnelProfile) {
        val intent = Intent(this, VpnForegroundService::class.java)
            .setAction(VpnForegroundService.ACTION_CONNECT)
            .putExtra(VpnForegroundService.EXTRA_PROFILE_ID, profile.id)
            .putExtra(VpnForegroundService.EXTRA_PROFILE_NAME, profile.name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@androidx.compose.runtime.Composable
private fun AppNavHost(
    navController: NavHostController,
    configRepository: ConfigRepository,
    settingsRepository: SettingsRepository,
    onRequestConnect: (TunnelProfile) -> Unit,
    onRequestDisconnect: () -> Unit
) {
    // Lindungi layar yang menampilkan/menerima private key dari screenshot & screen recording.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    androidx.compose.runtime.LaunchedEffect(currentRoute) {
        val sensitiveRoute = currentRoute == "manual_entry" || currentRoute == "qr_scan" || currentRoute == "add_config"
        if (sensitiveRoute) {
            activity?.window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                configRepository = configRepository,
                onRequestConnect = onRequestConnect,
                onRequestDisconnect = onRequestDisconnect,
                onNavigateAddConfig = { navController.navigate("add_config") },
                onNavigateConfigList = { navController.navigate("config_list") },
                onNavigateSettings = { navController.navigate("settings") }
            )
        }
        composable("add_config") {
            AddConfigScreen(
                onNavigateManual = { navController.navigate("manual_entry") },
                onNavigateQr = { navController.navigate("qr_scan") },
                onImported = { navController.popBackStack("home", inclusive = false) },
                configRepository = configRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable("manual_entry") {
            ManualEntryScreen(
                configRepository = configRepository,
                onSaved = { navController.popBackStack("home", inclusive = false) },
                onBack = { navController.popBackStack() }
            )
        }
        composable("qr_scan") {
            QrScanScreen(
                configRepository = configRepository,
                onSaved = { navController.popBackStack("home", inclusive = false) },
                onBack = { navController.popBackStack() }
            )
        }
        composable("config_list") {
            ConfigListScreen(
                configRepository = configRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                settingsRepository = settingsRepository,
                configRepository = configRepository,
                onNavigatePrivacyPolicy = { navController.navigate("privacy_policy") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("privacy_policy") {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
    }
}
