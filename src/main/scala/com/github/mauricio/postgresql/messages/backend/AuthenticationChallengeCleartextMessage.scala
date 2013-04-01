package com.github.mauricio.postgresql.messages.backend

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 1:31 AM
 */

object AuthenticationChallengeCleartextMessage {
  val Instance = new AuthenticationChallengeCleartextMessage()
}

class AuthenticationChallengeCleartextMessage
  extends AuthenticationChallengeMessage( AuthenticationResponseType.Cleartext, None )