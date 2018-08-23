package com.github.mauricio.async.db.mysql.message.client

import com.github.mauricio.async.db.mysql.message.server.AuthenticationSwitchRequest

case class AuthenticationSwitchResponse( password : Option[String], request : AuthenticationSwitchRequest )
  extends ClientMessage(ClientMessage.AuthSwitchResponse)