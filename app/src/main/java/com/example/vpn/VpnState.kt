package com.example.vpn

enum class VpnStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class VpnState(
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val duration: Long = 0L,
    val downloadSpeed: Long = 0L,
    val uploadSpeed: Long = 0L,
    val selectedProfile: String = "Default Profile",
    val errorMessage: String? = null
)
