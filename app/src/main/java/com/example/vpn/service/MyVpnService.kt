package com.example.vpn.service

import android.content.Context
import android.util.Base64
import com.tim.openvpn.configuration.OpenVPNConfig
import com.tim.openvpn.service.OpenVPNService
import java.nio.charset.StandardCharsets

object MyVpnService {
    fun start(context: Context, configBase64: String) {
        try {
            val decodedBytes = Base64.decode(configBase64, Base64.DEFAULT)
            val configString = String(decodedBytes, StandardCharsets.UTF_8)
            val config = OpenVPNConfig(configString)

            // Start OpenVPNService via its Companion object
            OpenVPNService.startService(context, config, "ModernVPN", arrayOf())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop(context: Context) {
        OpenVPNService.stopService(context)
    }
}
