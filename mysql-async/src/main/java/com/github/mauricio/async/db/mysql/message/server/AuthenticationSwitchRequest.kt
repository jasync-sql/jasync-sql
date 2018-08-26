package com.github.mauricio.async.db.mysql.message.server

data class AuthenticationSwitchRequest(
                                        val method : String,
                                        val seed : String )
  : ServerMessage(ServerMessage.EOF)
