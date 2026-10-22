package com.quietwire.tv.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.quietwire.tv.data.InstalledApp
import com.quietwire.tv.data.InstalledAppsProvider
import com.quietwire.tv.data.SecurePrefs

@Composable
fun SettingsScreen(viewModel: VpnViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var autoConnect by remember { mutableStateOf(viewModel.isAutoConnectEnabled()) }
    var splitMode by remember { mutableStateOf(SecurePrefs.getSplitTunnelMode(context)) }
    val apps = remember { InstalledAppsProvider.listUserFacingApps(context) }
    var selectedApps by remember { mutableStateOf(SecurePrefs.getSplitTunnelApps(context)) }

    Column(modifier = Modifier.fillMaxSize().padding(48.dp)) {
        Text(text = "Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        SettingRow(
            label = "Auto-connect on boot",
            value = if (autoConnect) "On" else "Off",
            onToggle = {
                autoConnect = !autoConnect
                viewModel.toggleAutoConnect(autoConnect)
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingRow(
            label = "Split tunneling mode",
            value = when (splitMode) {
                SecurePrefs.SplitTunnelMode.ALL -> "All apps use VPN"
                SecurePrefs.SplitTunnelMode.ALLOW_LISTED -> "Only selected apps"
                SecurePrefs.SplitTunnelMode.DISALLOW_LISTED -> "All except selected apps"
            },
            onToggle = {
                splitMode = when (splitMode) {
                    SecurePrefs.SplitTunnelMode.ALL -> SecurePrefs.SplitTunnelMode.ALLOW_LISTED
                    SecurePrefs.SplitTunnelMode.ALLOW_LISTED -> SecurePrefs.SplitTunnelMode.DISALLOW_LISTED
                    SecurePrefs.SplitTunnelMode.DISALLOW_LISTED -> SecurePrefs.SplitTunnelMode.ALL
                }
                SecurePrefs.setSplitTunnelMode(context, splitMode)
            },
        )

        if (splitMode != SecurePrefs.SplitTunnelMode.ALL) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Choose apps:", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.height(240.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(apps) { app ->
                    AppToggleRow(
                        app = app,
                        checked = selectedApps.contains(app.packageName),
                        onToggle = {
                            selectedApps = if (selectedApps.contains(app.packageName)) {
                                selectedApps - app.packageName
                            } else {
                                selectedApps + app.packageName
                            }
                            SecurePrefs.setSplitTunnelApps(context, selectedApps)
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "For a true kill switch, enable \"Always-on VPN\" + \"Block connections " +
                "without VPN\" for Quietwire in Android's system VPN settings.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
        }) {
            Text("Open system VPN settings")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun SettingRow(label: String, value: String, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 18.sp)
        Button(onClick = onToggle) { Text(value) }
    }
}

@Composable
private fun AppToggleRow(app: InstalledApp, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = app.label, fontSize = 14.sp)
        Button(
            onClick = onToggle,
            colors = ButtonDefaults.colors(
                containerColor = if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
            ),
        ) {
            Text(if (checked) "On" else "Off")
        }
    }
}
