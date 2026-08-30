package com.example.vpn

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vpn.service.MyVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class VpnViewModel(application: Application) : AndroidViewModel(application) {
    private val _vpnState = MutableStateFlow(VpnState())
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    fun setAutoMode(enabled: Boolean) {
        _vpnState.update { it.copy(isAutoMode = enabled) }
    }

    fun connect() {
        if (_vpnState.value.status == VpnStatus.CONNECTED || _vpnState.value.status == VpnStatus.CONNECTING) return

        viewModelScope.launch {
            _vpnState.update { it.copy(status = VpnStatus.CONNECTING, errorMessage = null) }

            if (_vpnState.value.isAutoMode) {
                delay(1000) // Simulate finding best route
            }

            _vpnState.update { it.copy(status = VpnStatus.VERIFYING) }
            delay(800) // Simulate verification

            val context = getApplication<Application>().applicationContext
            val intent = Intent(context, MyVpnService::class.java).apply {
                action = MyVpnService.ACTION_CONNECT
            }
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION.SDK_INT) {
                     context.startService(intent) // Changed from startForegroundService to avoid strict permission issues temporarily while testing dummy VPN
                }

                // Simulate getting valid connection metrics only after real connection happens
                _vpnState.update {
                    it.copy(
                        status = VpnStatus.CONNECTED,
                        ping = (20..80).random().toLong(),
                        downloadSpeed = 0L,
                        uploadSpeed = 0L
                    )
                }
                startActiveLoop()
            } catch (e: Exception) {
                _vpnState.update { it.copy(status = VpnStatus.ERROR, errorMessage = "Connection failed: ${e.message}") }
            }
        }
    }

    fun disconnect() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_DISCONNECT
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _vpnState.update {
            it.copy(
                status = VpnStatus.DISCONNECTED,
                duration = 0L,
                ping = 0L,
                downloadSpeed = 0L,
                uploadSpeed = 0L
            )
        }
    }

    private fun startActiveLoop() {
        viewModelScope.launch {
            while (_vpnState.value.status == VpnStatus.CONNECTED) {
                delay(1000L)
                _vpnState.update {
                    it.copy(
                        duration = it.duration + 1,
                        downloadSpeed = (10..500).random().toLong(),
                        uploadSpeed = (5..150).random().toLong()
                    )
                }
            }
        }
    }
}
