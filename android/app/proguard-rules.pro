# WireGuard's Go backend uses JNI + reflection; keep its classes intact.
-keep class com.wireguard.** { *; }
-keep class com.wireguard.android.backend.GoBackend$VpnService { *; }
-dontwarn com.wireguard.**
