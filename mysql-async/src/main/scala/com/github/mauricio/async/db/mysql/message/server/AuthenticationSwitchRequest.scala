package com.github.mauricio.async.db.mysql.message.server

case class AuthenticationSwitchRequest(
                                        method : String,
                                        seed : String )
  extends ServerMessage(ServerMessage.EOF)
