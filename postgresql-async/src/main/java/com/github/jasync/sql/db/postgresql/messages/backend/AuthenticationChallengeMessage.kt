package com.github.jasync.sql.db.postgresql.messages.backend

open class AuthenticationChallengeMessage(val challengeType: AuthenticationResponseType, val salt: ByteArray? = null) :
    AuthenticationMessage()
