package com.github.jasync.sql.db.postgresql.messages.backend

class AuthenticationChallengeMD5(salt: ByteArray) : AuthenticationChallengeMessage(AuthenticationResponseType.MD5, salt)
