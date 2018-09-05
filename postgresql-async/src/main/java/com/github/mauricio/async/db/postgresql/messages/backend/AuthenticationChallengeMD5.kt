package com.github.mauricio.async.db.postgresql.messages.backend

class AuthenticationChallengeMD5(salt: ByteArray) : AuthenticationChallengeMessage(AuthenticationResponseType.MD5, salt)
