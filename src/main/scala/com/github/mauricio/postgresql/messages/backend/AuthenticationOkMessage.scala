package com.github.mauricio.postgresql.messages.backend

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 1:30 AM
 */

object AuthenticationOkMessage {
  val Instance = new AuthenticationOkMessage()
}

class AuthenticationOkMessage extends AuthenticationMessage
