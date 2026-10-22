package com.quietwire.tv.vpn

import com.quietwire.tv.data.SecurePrefs
import com.quietwire.tv.data.ServerProfile
import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.Peer

/** Builds a com.wireguard.config.Config from our ServerProfile model. */
object WgConfigFactory {

    /**
     * splitTunnel: null means "route everything" (no split tunneling).
     * Otherwise it's the set of package names plus whether that set is the
     * allow-list (ONLY these use the VPN) or deny-list (everything EXCEPT
     * these uses the VPN). GoBackend turns these into
     * VpnService.Builder.addAllowedApplication/addDisallowedApplication calls.
     */
    fun build(
        profile: ServerProfile,
        splitTunnelMode: SecurePrefs.SplitTunnelMode = SecurePrefs.SplitTunnelMode.ALL,
        splitTunnelApps: Set<String> = emptySet(),
    ): Config {
        val ifaceBuilder = Interface.Builder()
            .parsePrivateKey(profile.clientPrivateKey)
            .parseAddresses(profile.clientAddress)
            .parseDnsServers(profile.dns)

        when (splitTunnelMode) {
            SecurePrefs.SplitTunnelMode.ALLOW_LISTED ->
                if (splitTunnelApps.isNotEmpty()) ifaceBuilder.parseIncludedApplications(splitTunnelApps.joinToString(","))
            SecurePrefs.SplitTunnelMode.DISALLOW_LISTED ->
                if (splitTunnelApps.isNotEmpty()) ifaceBuilder.parseExcludedApplications(splitTunnelApps.joinToString(","))
            SecurePrefs.SplitTunnelMode.ALL -> Unit
        }

        val iface = ifaceBuilder.build()

        val peerBuilder = Peer.Builder()
            .parsePublicKey(profile.serverPublicKey)
            .parseEndpoint("${profile.endpointHost}:${profile.endpointPort}")
            .parseAllowedIPs("0.0.0.0/0, ::/0")
            .setPersistentKeepalive(25)

        profile.presharedKey?.let { peerBuilder.parsePreSharedKey(it) }

        return Config.Builder()
            .setInterface(iface)
            .addPeer(peerBuilder.build())
            .build()
    }
}
