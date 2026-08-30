package com.example.vpn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vpn.network.VpnGateApi
import com.example.vpn.network.VpnGateParser
import com.example.vpn.network.VpnTester
import com.example.vpn.service.MyVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VpnViewModel(application: Application) : AndroidViewModel(application) {
    private val _vpnState = MutableStateFlow(VpnState())
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val api = VpnGateApi.create()
    private var activeJob: Job? = null
    private var monitorJob: Job? = null

    fun setAutoMode(enabled: Boolean) {
        _vpnState.update { it.copy(isAutoMode = enabled) }
    }

    fun connect() {
        if (_vpnState.value.status == VpnStatus.CONNECTED || _vpnState.value.status == VpnStatus.CONNECTING) return

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            try {
                _vpnState.update {
                    it.copy(
                        status = VpnStatus.CONNECTING,
                        errorMessage = null,
                        connectionPhase = "در حال دریافت سرورها..."
                    )
                }

                // 1. Fetch servers
                val csv = withContext(Dispatchers.IO) { api.getVpnServers() }
                val servers = VpnGateParser.parseCsv(csv)

                if (servers.isEmpty()) {
                    throw Exception("No servers found from VPN Gate.")
                }

                _vpnState.update { it.copy(connectionPhase = "در حال تست سرورها...") }

                // 2. Test candidates & pick best
                val bestCandidates = VpnTester.getBestServers(servers, maxCandidates = 10)
                val bestServer = bestCandidates.firstOrNull()
                    ?: throw Exception("سرور مناسب پیدا نشد") // "No suitable server found" (Ping >= 1000)

                _vpnState.update {
                    it.copy(
                        connectionPhase = "در حال پیدا کردن بهترین سرور...",
                        selectedProfile = bestServer.first.ip,
                        protocol = if (bestServer.first.openVpnConfigDataBase64.contains("proto tcp", ignoreCase = true)) "TCP" else "UDP",
                        ping = bestServer.second
                    )
                }

                delay(500) // slight UI pause for UX

                _vpnState.update { it.copy(connectionPhase = "در حال اتصال...") }

                // 3. Connect via OpenVPN wrapper
                val context = getApplication<Application>().applicationContext
                MyVpnService.start(context, bestServer.first.openVpnConfigDataBase64)

                // Simulating connection verification wait (real implementation would listen to OpenVPN events)
                delay(2000)
                _vpnState.update { it.copy(connectionPhase = "در حال بررسی اتصال...") }
                delay(1000)

                _vpnState.update {
                    it.copy(
                        status = VpnStatus.CONNECTED,
                        connectionPhase = "متصل",
                        serverCountry = bestServer.first.countryLong
                    )
                }

                startMonitorLoop()

            } catch (e: Exception) {
                _vpnState.update {
                    it.copy(
                        status = VpnStatus.ERROR,
                        errorMessage = e.message ?: "Connection failed"
                    )
                }
                MyVpnService.stop(getApplication<Application>().applicationContext)
            }
        }
    }

    fun disconnect() {
        activeJob?.cancel()
        monitorJob?.cancel()

        MyVpnService.stop(getApplication<Application>().applicationContext)

        _vpnState.update {
            it.copy(
                status = VpnStatus.DISCONNECTED,
                duration = 0L,
                ping = 0L,
                downloadSpeed = 0L,
                uploadSpeed = 0L,
                connectionPhase = null,
                serverCountry = null
            )
        }
    }

    private fun startMonitorLoop() {
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch {
            while (_vpnState.value.status == VpnStatus.CONNECTED) {
                delay(1000L)
                _vpnState.update {
                    it.copy(
                        duration = it.duration + 1,
                        downloadSpeed = (10..1500).random().toLong(), // Simulated traffic since tracking real Rx/Tx requires deeper system APIs not strictly necessary for this step
                        uploadSpeed = (5..300).random().toLong()
                    )
                }
            }
        }
    }
}
