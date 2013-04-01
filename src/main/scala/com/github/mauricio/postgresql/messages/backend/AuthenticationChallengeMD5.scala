package com.github.mauricio.postgresql.messages.backend

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 1:32 AM
 */

class AuthenticationChallengeMD5( salt : Array[Byte] )
  extends AuthenticationChallengeMessage( AuthenticationResponseType.MD5, Some(salt) )