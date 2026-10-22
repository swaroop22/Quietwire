package com.quietwire.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Android apps can't send raw ICMP pings without root, so we measure latency
 * as a TCP connect-time handshake to the server's WireGuard UDP port's host
 * (actually any open TCP port works as a proxy for round-trip time; we use
 * port 22/SSH since every VPS has it open — swap for a dedicated TCP health
 * check port if you prefer not to expose SSH logic here).
 */
object LatencyChecker {
    private const val PROBE_PORT = 22
    private const val TIMEOUT_MS = 1500

    suspend fun measureMillis(host: String): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val start = System.nanoTime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, PROBE_PORT), TIMEOUT_MS)
            }
            (System.nanoTime() - start) / 1_000_000
        }.getOrNull()
    }
}
