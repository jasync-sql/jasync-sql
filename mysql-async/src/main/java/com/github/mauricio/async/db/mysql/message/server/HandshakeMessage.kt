
package com.github.mauricio.async.db.mysql.message.server

data class HandshakeMessage(
    val serverVersion: String,
    val connectionId: Long,
    val seed: ByteArray,
    val serverCapabilities: Int,
    val characterSet: Int,
    val statusFlags: Int,
    val authenticationMethod : String
                             )
  : ServerMessage(ServerMessage.ServerProtocolVersion)
