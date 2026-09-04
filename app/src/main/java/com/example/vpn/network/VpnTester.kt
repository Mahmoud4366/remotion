package com.example.vpn.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import kotlin.system.measureTimeMillis

object VpnTester {
    suspend fun measurePing(ip: String): Long = withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(ip)
            val time = measureTimeMillis {
                val isReachable = address.isReachable(1000)
                if (!isReachable) throw Exception("Unreachable")
            }
            time
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }

    suspend fun getBestServers(servers: List<VpnServerModel>, maxCandidates: Int = 10): List<Pair<VpnServerModel, Long>> {
        val candidates = servers
            .filter { it.openVpnConfigDataBase64.isNotEmpty() }
            .sortedByDescending { it.score }
            .take(maxCandidates)

        val results = mutableListOf<Pair<VpnServerModel, Long>>()

        for (server in candidates) {
            val actualPing = measurePing(server.ip)
            if (actualPing < 1000L) {
                results.add(Pair(server, actualPing))
            }
        }

        return results.sortedBy { it.second }
    }
}
