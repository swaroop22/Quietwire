package com.quietwire.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.quietwire.tv.vpn.VpnConnectionState

@Composable
fun ConnectScreen(
    viewModel: VpnViewModel,
    onConnectRequested: () -> Unit,
    onOpenServers: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val selectedServerId by viewModel.selectedServerId.collectAsState()
    val selectedServer = servers.firstOrNull { it.id == selectedServerId }

    Box(modifier = Modifier.fillMaxSize().padding(48.dp)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = "Quietwire",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(24.dp))

            StatusDot(state = uiState.connectionState)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (uiState.connectionState) {
                    VpnConnectionState.CONNECTED -> "Connected"
                    VpnConnectionState.CONNECTING -> "Connecting…"
                    VpnConnectionState.ERROR -> "Connection error"
                    VpnConnectionState.DISCONNECTED -> "Not connected"
                },
                fontSize = 20.sp,
            )

            uiState.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = selectedServer?.displayName ?: "No server selected",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(40.dp))

            val isConnected = uiState.connectionState == VpnConnectionState.CONNECTED
            val isBusy = uiState.connectionState == VpnConnectionState.CONNECTING

            Button(
                onClick = {
                    if (isConnected) viewModel.disconnect() else onConnectRequested()
                },
                enabled = !isBusy && selectedServer != null,
                colors = ButtonDefaults.colors(
                    containerColor = if (isConnected) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.size(width = 260.dp, height = 72.dp),
            ) {
                Text(
                    text = if (isConnected) "Disconnect" else "Connect",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Button(onClick = onOpenServers) { Text("Servers") }
                Button(onClick = onOpenSettings) { Text("Settings") }
            }
        }
    }
}

@Composable
private fun StatusDot(state: VpnConnectionState) {
    val color = when (state) {
        VpnConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
        VpnConnectionState.CONNECTING -> MaterialTheme.colorScheme.tertiary
        VpnConnectionState.ERROR -> MaterialTheme.colorScheme.error
        VpnConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color),
    )
}
