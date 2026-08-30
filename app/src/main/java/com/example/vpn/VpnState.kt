package com.example.vpn

enum class VpnStatus {
    DISCONNECTED,
    CONNECTING,
    VERIFYING,
    CONNECTED,
    ERROR
}

data class VpnState(
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val duration: Long = 0L,
    val downloadSpeed: Long = 0L,
    val uploadSpeed: Long = 0L,
    val ping: Long = 0L,
    val selectedProfile: String = "Default",
    val protocol: String = "UDP",
    val isAutoMode: Boolean = false,
    val dnsStatus: String = "1.1.1.1",
    val errorMessage: String? = null,
    val connectionPhase: String? = null,
    val serverCountry: String? = null
)
