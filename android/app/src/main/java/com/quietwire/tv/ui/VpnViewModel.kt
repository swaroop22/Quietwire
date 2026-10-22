package com.quietwire.tv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quietwire.tv.data.LatencyChecker
import com.quietwire.tv.data.SecurePrefs
import com.quietwire.tv.data.ServerCatalog
import com.quietwire.tv.data.ServerProfile
import com.quietwire.tv.vpn.VpnManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VpnViewModel(application: Application) : AndroidViewModel(application) {
    val vpnManager = VpnManager(application)
    val uiState = vpnManager.uiState

    private val _servers = MutableStateFlow(ServerCatalog.servers)
    val servers: StateFlow<List<ServerProfile>> = _servers.asStateFlow()

    private val _pingResults = MutableStateFlow<Map<String, Long?>>(emptyMap())
    val pingResults: StateFlow<Map<String, Long?>> = _pingResults.asStateFlow()

    private val _selectedServerId = MutableStateFlow(
        SecurePrefs.getLastServerId(application) ?: ServerCatalog.servers.firstOrNull()?.id,
    )
    val selectedServerId: StateFlow<String?> = _selectedServerId.asStateFlow()

    init {
        refreshLatencies()
    }

    fun selectServer(server: ServerProfile) {
        _selectedServerId.value = server.id
        SecurePrefs.setLastServerId(getApplication(), server.id)
    }

    fun connect() {
        val server = _servers.value.firstOrNull { it.id == _selectedServerId.value } ?: return
        vpnManager.connect(server)
    }

    fun disconnect() = vpnManager.disconnect()

    fun toggleAutoConnect(enabled: Boolean) =
        SecurePrefs.setAutoConnectEnabled(getApplication(), enabled)

    fun isAutoConnectEnabled(): Boolean = SecurePrefs.isAutoConnectEnabled(getApplication())

    private fun refreshLatencies() {
        viewModelScope.launch {
            val results = mutableMapOf<String, Long?>()
            _servers.value.forEach { server ->
                results[server.id] = LatencyChecker.measureMillis(server.endpointHost)
                _pingResults.value = results.toMap()
            }
        }
    }
}
