package com.quietwire.tv.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.quietwire.tv.data.ServerProfile
import com.quietwire.tv.vpn.VpnManager

enum class Screen { CONNECT, SERVERS, SETTINGS }

class MainActivity : ComponentActivity() {

    private val viewModel: VpnViewModel by viewModels()
    private var pendingServer: ServerProfile? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.connect()
        }
        pendingServer = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var screen by remember { mutableStateOf(Screen.CONNECT) }

            QuietwireTheme {
                when (screen) {
                    Screen.CONNECT -> ConnectScreen(
                        viewModel = viewModel,
                        onConnectRequested = { requestConnect() },
                        onOpenServers = { screen = Screen.SERVERS },
                        onOpenSettings = { screen = Screen.SETTINGS },
                    )
                    Screen.SERVERS -> ServerListScreen(
                        viewModel = viewModel,
                        onBack = { screen = Screen.CONNECT },
                    )
                    Screen.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onBack = { screen = Screen.CONNECT },
                    )
                }
            }
        }
    }

    /**
     * Android requires VpnService.prepare() to be called from an Activity
     * (it may show a system "allow this app to create a VPN connection"
     * dialog the very first time). Only after the user grants that can we
     * call VpnManager.connect().
     */
    private fun requestConnect() {
        val intent: Intent? = VpnManager.prepareIntent(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            viewModel.connect()
        }
    }
}
