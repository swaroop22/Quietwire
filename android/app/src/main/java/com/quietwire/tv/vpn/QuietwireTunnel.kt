package com.quietwire.tv.vpn

import com.wireguard.android.backend.Tunnel

/**
 * Minimal Tunnel implementation required by GoBackend. GoBackend calls
 * onStateChange whenever the tunnel's up/down state changes (including
 * unexpected drops), which is how VpnManager keeps its StateFlow in sync.
 */
class QuietwireTunnel(
    private val name: String,
    private val onStateChange: (Tunnel.State) -> Unit,
) : Tunnel {
    override fun getName(): String = name

    override fun onStateChange(newState: Tunnel.State) {
        onStateChange.invoke(newState)
    }
}
