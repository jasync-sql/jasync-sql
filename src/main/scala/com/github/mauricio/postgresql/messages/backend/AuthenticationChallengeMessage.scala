package com.github.mauricio.postgresql.messages.backend

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 1:45 AM
 */
class AuthenticationChallengeMessage ( val challengeType : AuthenticationResponseType.AuthenticationResponseType )
  extends AuthenticationMessage