package com.github.jasync.sql.db.postgresql.messages.backend

class AuthenticationMD5PasswordMessage(val salt: ByteArray) : AuthenticationSimpleChallenge()
