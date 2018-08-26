package com.github.mauricio.async.db.mysql.message.client

import com.github.mauricio.async.db.mysql.message.server.AuthenticationSwitchRequest

data class AuthenticationSwitchResponse( val password : String?, val request : AuthenticationSwitchRequest )
  : ClientMessage(ClientMessage.AuthSwitchResponse)
