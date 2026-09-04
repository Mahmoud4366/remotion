package com.example.vpn.network

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET

interface VpnGateApi {
    @GET("api/iphone/")
    suspend fun getVpnServers(): String

    companion object {
        fun create(): VpnGateApi {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://www.vpngate.net/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
            return retrofit.create(VpnGateApi::class.java)
        }
    }
}

object VpnGateParser {
    fun parseCsv(csvData: String): List<VpnServerModel> {
        val servers = mutableListOf<VpnServerModel>()
        val lines = csvData.split("\n")

        for (line in lines) {
            // Skip comments and header
            if (line.startsWith("*") || line.startsWith("#") || line.trim().isEmpty()) {
                continue
            }

            val parts = line.split(",")
            if (parts.size >= 15) {
                try {
                    servers.add(
                        VpnServerModel(
                            hostName = parts[0],
                            ip = parts[1],
                            score = parts[2].toIntOrNull() ?: 0,
                            ping = parts[3].toLongOrNull() ?: 0L,
                            speed = parts[4].toLongOrNull() ?: 0L,
                            countryLong = parts[5],
                            countryShort = parts[6],
                            numVpnSessions = parts[7].toIntOrNull() ?: 0,
                            uptime = parts[8].toLongOrNull() ?: 0L,
                            totalUsers = parts[9].toLongOrNull() ?: 0L,
                            totalTraffic = parts[10].toLongOrNull() ?: 0L,
                            logType = parts[11],
                            operator = parts[12],
                            message = parts[13],
                            openVpnConfigDataBase64 = parts[14]
                        )
                    )
                } catch (e: Exception) {
                    // Ignore malformed rows
                }
            }
        }
        return servers
    }
}
