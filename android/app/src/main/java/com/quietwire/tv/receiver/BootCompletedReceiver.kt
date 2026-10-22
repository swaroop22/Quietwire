package com.quietwire.tv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.quietwire.tv.data.SecurePrefs
import com.quietwire.tv.data.ServerCatalog
import com.quietwire.tv.vpn.VpnManager

/**
 * Auto-connect on boot. Note: this only works if VPN permission was already
 * granted in a prior foreground session — VpnService.prepare() requires UI
 * and cannot be shown from a boot receiver. If the user has never opened the
 * app and tapped Connect once, this silently does nothing (expected Android
 * behavior, not a bug).
 *
 * For a true "kill switch" (block all traffic until VPN is up), combine this
 * with Android's system-level Settings > Network > Advanced > VPN >
 * "Always-on VPN" + "Block connections without VPN" toggle on this app —
 * see Part 3 notes on kill switch.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!SecurePrefs.isAutoConnectEnabled(context)) return

        // VPN permission already granted previously (prepare() returns null)?
        if (VpnManager.prepareIntent(context) != null) return

        val lastServerId = SecurePrefs.getLastServerId(context)
        val server = ServerCatalog.servers.firstOrNull { it.id == lastServerId }
            ?: ServerCatalog.servers.firstOrNull()
            ?: return

        VpnManager(context).connect(server)
    }
}
