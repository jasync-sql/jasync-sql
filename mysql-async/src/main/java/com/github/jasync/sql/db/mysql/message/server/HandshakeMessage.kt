package com.github.jasync.sql.db.mysql.message.server

data class HandshakeMessage(
    val serverVersion: String,
    val connectionId: Long,
    @Suppress("ArrayInDataClass")
    val seed: ByteArray,
    val serverCapabilities: Int,
    val characterSet: Int,
    val statusFlags: Int,
    val authenticationMethod: String
) : ServerMessage(ServerMessage.ServerProtocolVersion)
