package com.quietwire.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.quietwire.tv.data.ServerProfile

@Composable
fun ServerListScreen(viewModel: VpnViewModel, onBack: () -> Unit) {
    val servers by viewModel.servers.collectAsState()
    val pingResults by viewModel.pingResults.collectAsState()
    val selectedServerId by viewModel.selectedServerId.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(48.dp)) {
        Text(text = "Select a server", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(servers) { server ->
                ServerRow(
                    server = server,
                    pingMs = pingResults[server.id],
                    isSelected = server.id == selectedServerId,
                    onSelect = { viewModel.selectServer(server) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun ServerRow(
    server: ServerProfile,
    pingMs: Long?,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Button(
        onClick = onSelect,
        colors = ButtonDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth().height(64.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(text = server.displayName, fontSize = 18.sp)
                Text(text = server.countryCode, fontSize = 12.sp)
            }
            Box {
                Text(
                    text = pingMs?.let { "${it} ms" } ?: "…",
                    fontSize = 16.sp,
                )
            }
        }
    }
}
