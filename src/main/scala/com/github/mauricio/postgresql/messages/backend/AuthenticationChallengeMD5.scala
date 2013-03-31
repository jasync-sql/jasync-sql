package com.github.mauricio.postgresql.messages.backend

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 1:32 AM
 */

object AuthenticationChallengeMD5 {
  val Instance = new AuthenticationChallengeMD5()
}

class AuthenticationChallengeMD5
  extends AuthenticationChallengeMessage( AuthenticationResponseType.MD5 )