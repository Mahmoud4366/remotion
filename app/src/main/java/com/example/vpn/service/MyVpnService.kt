package com.example.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.vpn.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var vpnJob: Job? = null

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.DISCONNECT"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "VpnServiceChannel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> connect()
            ACTION_DISCONNECT -> disconnect()
        }
        return START_NOT_STICKY
    }

    private fun connect() {
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { startForeground(NOTIFICATION_ID, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) } else { startForeground(NOTIFICATION_ID, createNotification()) }

        vpnJob?.cancel()
        vpnJob = serviceScope.launch {
            try {
                // In a real scenario, network setup & handshake goes here.
                // We'll set up a dummy VpnInterface to satisfy Android API.
                setupVpnInterface()
                runVpnLoop()
            } catch (e: Exception) {
                e.printStackTrace()
                disconnect()
            }
        }
    }

    private fun setupVpnInterface() {
        val builder = Builder()
            .addAddress("10.0.0.2", 24)
            .addRoute("0.0.0.0", 0)
            .setSession("ModernVPN")
            .setConfigureIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

        // Only block traffic if possible, this is a simplified kill switch idea
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        vpnInterface = builder.establish()
    }

    private suspend fun runVpnLoop() {
        vpnInterface?.let { vpn ->
            val inputStream = FileInputStream(vpn.fileDescriptor)
            val outputStream = FileOutputStream(vpn.fileDescriptor)
            val packet = ByteArray(32767)

            try {
                while (true) {
                    val length = inputStream.read(packet)
                    if (length > 0) {
                        // Normally encrypt and forward to external server here.
                        // We do nothing for now as there's no real server.
                    }
                    delay(10) // Prevent high CPU usage in this dummy loop
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                inputStream.close()
                outputStream.close()
            }
        }
    }

    private fun disconnect() {
        vpnJob?.cancel()
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ModernVPN")
            .setContentText("VPN is connected")
            .setSmallIcon(android.R.drawable.ic_secure) // Generic icon
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        disconnect()
    }
}
