package com.github.jasync.sql.db.mysql.message.server

import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_SSL

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
        return CLIENT_SSL and serverCapabilities != 0
    }

}
