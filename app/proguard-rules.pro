# ADJDEV VPN - ProGuard rules
# Keep WireGuard tunnel backend classes (JNI bridge to wireguard-go).
-keep class com.wireguard.android.backend.** { *; }
-keep class com.wireguard.config.** { *; }
-keep class com.wireguard.crypto.** { *; }

# Keep Compose runtime
-keep class androidx.compose.** { *; }
