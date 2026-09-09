package id.adjdev.vpn.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import id.adjdev.vpn.R
import id.adjdev.vpn.data.ConfigParseResult
import id.adjdev.vpn.data.ConfigParser
import id.adjdev.vpn.data.ConfigRepository
import id.adjdev.vpn.ui.theme.StatusFailed
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun QrScanScreen(
    configRepository: ConfigRepository,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    var manualQrText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var handled by remember { mutableStateOf(false) }

    fun tryApply(text: String) {
        if (handled) return
        when (val result = ConfigParser.parse(text)) {
            is ConfigParseResult.Success -> {
                handled = true
                configRepository.saveNewProfile("Konfigurasi QR", result.config)
                onSaved()
            }
            is ConfigParseResult.Error -> {
                errorMessage = result.messageId.messageIndonesian()
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResourceCompat(R.string.menu_scan_qr)) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (!hasCameraPermission) {
                Text(stringResourceCompat(R.string.error_camera_permission))
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Berikan izin kamera")
                }
            } else {
                CameraQrPreview(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    onDecoded = ::tryApply
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            Text("Atau tempel teks konfigurasi hasil QR secara manual:")
            OutlinedTextField(
                value = manualQrText,
                onValueChange = { manualQrText = it },
                modifier = Modifier.fillMaxWidth().height(160.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { tryApply(manualQrText) }, modifier = Modifier.fillMaxWidth()) {
                Text("Gunakan teks ini")
            }

            errorMessage?.let {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                Text(text = it, color = StatusFailed)
            }
        }
    }
}

@Composable
private fun CameraQrPreview(modifier: Modifier, onDecoded: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QrAnalyzer(onDecoded))
                }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                // Kamera tidak tersedia / tidak didukung perangkat ini; pengguna tetap
                // dapat memakai fallback teks manual di bawah preview.
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
private fun stringResourceCompat(id: Int) = androidx.compose.ui.res.stringResource(id)
