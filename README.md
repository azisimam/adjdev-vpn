# ADJDEV VPN

Aplikasi klien VPN Android native yang terhubung ke server WireGuard sungguhan menggunakan `android.net.VpnService` dan backend resmi [`com.wireguard.android:tunnel`](https://github.com/WireGuard/wireguard-android) (implementasi WireGuard userspace `wireguard-go`). Aplikasi ini **bukan** simulasi, bukan WebView, dan bukan proxy palsu — tombol Connect hanya menampilkan status "Terhubung" setelah tunnel WireGuard benar-benar aktif.

## 1. Cara membuka project di Android Studio

1. Ekstrak ZIP ini.
2. Buka Android Studio → **Open** → pilih folder `ADJDEV-VPN/`.
3. Tunggu proses **Gradle Sync** selesai (memerlukan koneksi internet untuk mengunduh dependency pertama kali).
4. Jika `gradle/wrapper/gradle-wrapper.jar` belum ada di komputer Anda (lihat catatan di bagian 5), jalankan sekali:
   ```
   gradle wrapper --gradle-version 8.7
   ```
   menggunakan instalasi Gradle lokal, lalu buka ulang project.

## 2. Cara menjalankan aplikasi pada perangkat Android

1. Aktifkan **USB debugging** di perangkat Android (minimal Android 8.0 / API 26).
2. Hubungkan perangkat via USB atau gunakan emulator dengan Google Play Image (VPN memerlukan izin sistem yang tidak selalu tersedia di semua image emulator).
3. Klik **Run ▶** di Android Studio, pilih perangkat target.
4. Saat pertama kali menekan **CONNECT**, sistem Android akan menampilkan dialog izin VPN resmi — izinkan agar tunnel dapat dibuat.

## 3. Cara mengimpor file WireGuard

Dari halaman utama, pilih **Tambah Konfigurasi**, lalu:

- **Import dari file `.conf`** — memakai Android Storage Access Framework (SAF), tidak memerlukan izin penyimpanan luas.
- **Scan QR code** — arahkan kamera ke QR WireGuard, atau tempel teks hasil QR secara manual jika kamera tidak tersedia.
- **Isi konfigurasi manual** — form dengan field `PrivateKey`, `Address`, `DNS`, `PublicKey` (server), `Endpoint`, `AllowedIPs`, `PersistentKeepalive`.

Private key **tidak pernah** ditulis ke Logcat dan disimpan hanya di penyimpanan terenkripsi (`EncryptedSharedPreferences` yang dibungkus Android Keystore).

## 4. Cara memperoleh konfigurasi client yang benar

Template pada repository ini (`SERVER_PUBLIC_KEY`, `CLIENT_PRIVATE_KEY`) **bukan kunci nyata** dan harus diganti dengan kunci asli dari administrator server WireGuard Anda. Format standar:

```ini
[Interface]
PrivateKey = <private key client Anda>
Address = 10.8.0.2/32
DNS = 1.1.1.1

[Peer]
PublicKey = <public key server>
Endpoint = 148.230.96.102:51820
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
```

> **Peringatan keamanan penting**: jika Anda pernah membagikan sebuah `PrivateKey` di luar server WireGuard tujuannya (misalnya di chat, file yang diunggah ke pihak lain, atau repository publik), anggap key tersebut **bocor** dan buat pasangan key baru di server. Private key tidak boleh pernah ditanam di source code atau dikomit ke Git.

## 5. Cara menjalankan GitHub Actions

1. Push repository ini ke GitHub (branch `main` atau `master` akan otomatis memicu build).
2. Atau buka tab **Actions** → workflow **Build APK** → **Run workflow** (workflow_dispatch).
3. Workflow menjalankan pemeriksaan project, unit test, lalu `./gradlew assembleDebug`.

**Catatan jujur tentang `gradle-wrapper.jar`:** karena project ini disusun tanpa akses jaringan untuk mengunduh biner Gradle, file `gradle/wrapper/gradle-wrapper.jar` mungkin tidak tersedia di ZIP ini. Workflow CI sudah menangani ini secara otomatis (langkah "Ensure Gradle Wrapper jar exists" akan membuatnya memakai Gradle yang terpasang di runner `ubuntu-latest`). Jika Anda membuka project secara lokal dan Android Studio mengeluh wrapper tidak ada, jalankan `gradle wrapper --gradle-version 8.7` sekali seperti pada bagian 1.

## 6. Cara mengunduh artifact APK

Setelah workflow selesai (centang hijau), buka halaman run tersebut di tab **Actions**, lalu unduh artifact bernama **`adjdev-vpn-debug-apk`** di bagian bawah halaman.

## 7. Cara memasang APK di Android

1. Salin file `.apk` hasil unduhan ke perangkat Android.
2. Buka file tersebut dari File Manager (aktifkan "Izinkan dari sumber ini" jika diminta, karena APK debug tidak berasal dari Play Store).
3. Ikuti proses instalasi standar.

## 8. Cara menambahkan konfigurasi release signing

Release build memerlukan keystore Android yang Anda buat sendiri — repository ini **tidak menyertakan keystore apa pun**. Langkah:

1. Buat keystore lokal (jika belum punya):
   ```
   keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias adjdev
   ```
2. Encode ke base64:
   ```
   base64 -w0 release.jks > release.jks.base64
   ```
3. Di GitHub, buka **Settings → Secrets and variables → Actions**, tambahkan 4 secret:
   - `KEYSTORE_BASE64` — isi dari `release.jks.base64`
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`
4. Jalankan ulang workflow. Jika keempat secret sudah lengkap, job tambahan **Build release APK** akan berjalan dan mengunggah artifact `adjdev-vpn-release-apk`. Jika secret belum diisi, langkah ini otomatis dilewati dan build debug tetap berhasil seperti biasa.

## 9. Troubleshooting jika handshake belum terjadi

- Pastikan `Endpoint` (host dan port) dapat dijangkau dari jaringan perangkat Anda — coba `ping`/`nc` ke IP dan port tersebut dari jaringan yang sama.
- Pastikan `PublicKey` pada `[Peer]` cocok dengan public key server, dan `PrivateKey` pada `[Interface]` memang pasangan dari public key yang terdaftar di server.
- Beberapa jaringan seluler/ISP memblokir trafik UDP ke port non-standar — coba jaringan lain (mis. Wi-Fi) untuk memastikan.
- Field **Handshake terakhir** di halaman utama akan tetap kosong ("-") selama belum ada balasan dari server; ini bukan bug UI, melainkan cerminan langsung dari status tunnel.
- Aktifkan logcat (`adb logcat`) saat debug — aplikasi ini sengaja tidak pernah mencatat private key ke log, jadi log tetap aman dibagikan untuk troubleshooting.

## 10. Peringatan: jangan bagikan private key Anda

Private key pada `[Interface]` bersifat rahasia dan setara dengan kata sandi ke identitas VPN Anda. Jangan:

- Menempelkannya ke chat, forum, atau issue tracker publik.
- Mengomit file `.conf` asli ke Git.
- Membagikan screenshot layar konfigurasi (aplikasi ini berusaha melindungi layar konfigurasi dari screenshot bila memungkinkan, tetapi kewaspadaan pengguna tetap yang utama).

## Struktur project

```
ADJDEV-VPN/
├── app/                          # Source code aplikasi (Kotlin + Jetpack Compose)
├── gradle/wrapper/                # Gradle wrapper
├── .github/workflows/build-apk.yml
├── build.gradle.kts / settings.gradle.kts / gradle.properties
├── gradlew / gradlew.bat
└── README.md
```

## Status pengujian jujur

Kode ini disusun dan ditinjau secara manual, termasuk unit test untuk parser konfigurasi (`app/src/test/java/id/adjdev/vpn/ConfigParserTest.kt`), tetapi **belum pernah dijalankan `gradle sync` atau dibuild secara nyata di lingkungan ini** karena lingkungan pembuatan project tidak memiliki akses jaringan untuk mengunduh Gradle/Android SDK/dependency. Jangan menganggap aplikasi sudah terbukti berhasil terhubung ke server WireGuard sungguhan sampai Anda:

1. Menjalankan GitHub Actions dan memastikan `assembleDebug` serta unit test lulus tanpa error, dan
2. Menguji APK hasil build pada perangkat Android fisik dengan konfigurasi WireGuard nyata, dan mengonfirmasi status berubah menjadi **Terhubung** disertai handshake dan lalu lintas data yang benar-benar mengalir.

Jika Gradle Sync menemukan versi library yang sudah usang (misalnya versi `com.wireguard.android:tunnel` yang lebih baru tersedia), silakan perbarui nomor versi di `app/build.gradle.kts` — struktur kode di `TunnelManager.kt` mengikuti API publik resmi library tersebut per pengetahuan terakhir penulis dan mungkin perlu penyesuaian kecil jika API upstream berubah.

## Lisensi

Lihat berkas `LICENSE` (MIT).
