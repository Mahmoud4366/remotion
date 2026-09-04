package com.example.vpn.network

data class VpnServerModel(
    val hostName: String,
    val ip: String,
    val score: Int,
    val ping: Long,
    val speed: Long,
    val countryLong: String,
    val countryShort: String,
    val numVpnSessions: Int,
    val uptime: Long,
    val totalUsers: Long,
    val totalTraffic: Long,
    val logType: String,
    val operator: String,
    val message: String,
    val openVpnConfigDataBase64: String
)
