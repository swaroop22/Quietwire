package com.quietwire.tv.data

/**
 * One VPN location = one WireGuard peer config, produced by add-client.sh
 * on that server. You'll have one ServerProfile per country/server, each
 * holding the keys issued to THIS device by THAT server.
 */
data class ServerProfile(
    val id: String,
    val displayName: String,
    val countryCode: String,     // e.g. "DE", "US", "SG" — used for a flag emoji/icon
    val endpointHost: String,
    val endpointPort: Int,
    val serverPublicKey: String,
    val clientPrivateKey: String,
    val clientAddress: String,   // e.g. "10.66.0.2/32"
    val presharedKey: String? = null,
    val dns: String = "1.1.1.1",
)

/** Hardcode your own servers here once you've run add-client.sh on each VPS. */
object ServerCatalog {
    val servers: List<ServerProfile> = listOf(
        // Example entries — replace with real values from add-client.sh output.
        ServerProfile(
            id = "de-falkenstein",
            displayName = "Germany - Falkenstein",
            countryCode = "DE",
            endpointHost = "203.0.113.10",
            endpointPort = 51820,
            serverPublicKey = "SERVER_PUBLIC_KEY_HERE=",
            clientPrivateKey = "CLIENT_PRIVATE_KEY_HERE=",
            clientAddress = "10.66.0.2/32",
            presharedKey = "PRESHARED_KEY_HERE=",
        ),
        ServerProfile(
            id = "us-ashburn",
            displayName = "United States - Ashburn",
            countryCode = "US",
            endpointHost = "203.0.113.20",
            endpointPort = 51820,
            serverPublicKey = "SERVER_PUBLIC_KEY_HERE=",
            clientPrivateKey = "CLIENT_PRIVATE_KEY_HERE=",
            clientAddress = "10.66.0.2/32",
            presharedKey = "PRESHARED_KEY_HERE=",
        ),
    )
}
