package com.github.jasync.sql.db.mysql.message.server

import com.github.jasync.sql.db.mysql.util.CapabilityFlag

data class HandshakeMessage(
    val serverVersion: String,
    val connectionId: Long,
    @Suppress("ArrayInDataClass")
    val seed: ByteArray,
    val serverCapabilities: Int,
    val characterSet: Int,
    val statusFlags: Int,
    val authenticationMethod: String
) : ServerMessage(ServerProtocolVersion) {
    fun supportsSSL(): Boolean {
        return CapabilityFlag.CLIENT_SSL.value and serverCapabilities != 0
    }
}
