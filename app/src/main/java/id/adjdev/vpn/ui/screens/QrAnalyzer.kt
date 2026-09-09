package id.adjdev.vpn.ui.screens

import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/**
 * Menganalisis frame kamera untuk QR code secara lokal di perangkat.
 * Tidak ada gambar yang disimpan atau dikirim ke jaringan.
 */
class QrAnalyzer(private val onDecoded: (String) -> Unit) : androidx.camera.core.ImageAnalysis.Analyzer {

    private val reader = QRCodeReader()

    override fun analyze(image: ImageProxy) {
        try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val source = PlanarYUVLuminanceSource(
                bytes,
                image.width,
                image.height,
                0,
                0,
                image.width,
                image.height,
                false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decode(bitmap)
            onDecoded(result.text)
        } catch (e: NotFoundException) {
            // Tidak ada QR pada frame ini; lanjut ke frame berikutnya.
        } catch (e: Exception) {
            // Abaikan frame yang gagal didekode.
        } finally {
            image.close()
        }
    }
}
