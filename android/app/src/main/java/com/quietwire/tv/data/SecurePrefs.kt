package com.quietwire.tv.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted local store for user-chosen settings: last connected server,
 * split-tunnel app list, auto-connect flag. Server keys themselves live in
 * ServerCatalog for this starter app; if you later let users add custom
 * servers from the UI, store those configs here too (same AES-256-GCM file).
 */
object SecurePrefs {
    private const val FILE_NAME = "quietwire_secure_prefs"

    private const val KEY_LAST_SERVER_ID = "last_server_id"
    private const val KEY_AUTO_CONNECT = "auto_connect"
    private const val KEY_SPLIT_TUNNEL_MODE = "split_tunnel_mode" // "ALL" | "ALLOW" | "DISALLOW"
    private const val KEY_SPLIT_TUNNEL_APPS = "split_tunnel_apps" // comma-separated package names

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getLastServerId(context: Context): String? =
        prefs(context).getString(KEY_LAST_SERVER_ID, null)

    fun setLastServerId(context: Context, id: String) {
        prefs(context).edit().putString(KEY_LAST_SERVER_ID, id).apply()
    }

    fun isAutoConnectEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_CONNECT, false)

    fun setAutoConnectEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_CONNECT, enabled).apply()
    }

    enum class SplitTunnelMode { ALL, ALLOW_LISTED, DISALLOW_LISTED }

    fun getSplitTunnelMode(context: Context): SplitTunnelMode {
        val raw = prefs(context).getString(KEY_SPLIT_TUNNEL_MODE, SplitTunnelMode.ALL.name)
        return runCatching { SplitTunnelMode.valueOf(raw!!) }.getOrDefault(SplitTunnelMode.ALL)
    }

    fun setSplitTunnelMode(context: Context, mode: SplitTunnelMode) {
        prefs(context).edit().putString(KEY_SPLIT_TUNNEL_MODE, mode.name).apply()
    }

    fun getSplitTunnelApps(context: Context): Set<String> =
        prefs(context).getString(KEY_SPLIT_TUNNEL_APPS, "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()

    fun setSplitTunnelApps(context: Context, packages: Set<String>) {
        prefs(context).edit().putString(KEY_SPLIT_TUNNEL_APPS, packages.joinToString(",")).apply()
    }
}
