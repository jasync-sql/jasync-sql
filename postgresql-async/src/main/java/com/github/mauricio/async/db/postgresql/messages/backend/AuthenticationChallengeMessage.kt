
package com.github.mauricio.async.db.postgresql.messages.backend

open class AuthenticationChallengeMessage(val challengeType: AuthenticationResponseType, val salt: ByteArray? = null) : AuthenticationMessage()
