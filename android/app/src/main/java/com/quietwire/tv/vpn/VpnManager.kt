package com.quietwire.tv.vpn

import android.content.Context
import android.net.VpnService
import com.quietwire.tv.data.SecurePrefs
import com.quietwire.tv.data.ServerProfile
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VpnConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

data class VpnUiState(
    val connectionState: VpnConnectionState = VpnConnectionState.DISCONNECTED,
    val activeServer: ServerProfile? = null,
    val errorMessage: String? = null,
)

/**
 * Single entry point the UI talks to for connect/disconnect. Wraps WireGuard's
 * GoBackend (the userspace WireGuard implementation bundled in
 * com.wireguard.android:tunnel). GoBackend internally starts/stops its own
 * VpnService, so we never subclass VpnService ourselves.
 *
 * VpnService.prepare() must be called from an Activity first (see
 * MainActivity's vpnPermissionLauncher) — connect() assumes that permission
 * has already been granted, matching how the official WireGuard app does it.
 */
class VpnManager(context: Context) {
    private val appContext = context.applicationContext
    private val backend: Backend = GoBackend(appContext)

    private val _uiState = MutableStateFlow(VpnUiState())
    val uiState: StateFlow<VpnUiState> = _uiState.asStateFlow()

    private var tunnel: QuietwireTunnel? = null

    companion object {
        private const val TUNNEL_NAME = "quietwire0"

        /** Returns an Intent to launch for VPN permission, or null if already granted. */
        fun prepareIntent(context: Context) = VpnService.prepare(context)
    }

    fun connect(profile: ServerProfile) {
        _uiState.value = _uiState.value.copy(
            connectionState = VpnConnectionState.CONNECTING,
            activeServer = profile,
            errorMessage = null,
        )
        try {
            val splitMode = SecurePrefs.getSplitTunnelMode(appContext)
            val splitApps = SecurePrefs.getSplitTunnelApps(appContext)
            val config = WgConfigFactory.build(profile, splitMode, splitApps)

            val t = QuietwireTunnel(TUNNEL_NAME) { state -> onTunnelStateChanged(state) }
            tunnel = t
            backend.setState(t, Tunnel.State.UP, config)

            SecurePrefs.setLastServerId(appContext, profile.id)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                connectionState = VpnConnectionState.ERROR,
                errorMessage = e.message ?: "Failed to connect",
            )
        }
    }

    fun disconnect() {
        val t = tunnel ?: return
        try {
            backend.setState(t, Tunnel.State.DOWN, null)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = e.message)
        }
    }

    fun currentState(): VpnConnectionState = _uiState.value.connectionState

    private fun onTunnelStateChanged(state: Tunnel.State) {
        _uiState.value = _uiState.value.copy(
            connectionState = when (state) {
                Tunnel.State.UP -> VpnConnectionState.CONNECTED
                Tunnel.State.DOWN -> VpnConnectionState.DISCONNECTED
                Tunnel.State.TOGGLE -> VpnConnectionState.CONNECTING
            },
        )
    }
}
