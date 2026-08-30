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

    fun connect() {
        _vpnState.update { it.copy(status = VpnStatus.CONNECTING, errorMessage = null) }
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_CONNECT
        }
        try {
            context.startForegroundService(intent)
            _vpnState.update { it.copy(status = VpnStatus.CONNECTED) }
            startDurationTimer()
        } catch (e: Exception) {
            _vpnState.update { it.copy(status = VpnStatus.ERROR, errorMessage = "Connection failed: ${e.message}") }
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
        _vpnState.update { it.copy(status = VpnStatus.DISCONNECTED, duration = 0L) }
    }

    private fun startDurationTimer() {
        viewModelScope.launch {
            while (_vpnState.value.status == VpnStatus.CONNECTED) {
                delay(1000L)
                _vpnState.update { it.copy(duration = it.duration + 1) }
            }
        }
    }
}
