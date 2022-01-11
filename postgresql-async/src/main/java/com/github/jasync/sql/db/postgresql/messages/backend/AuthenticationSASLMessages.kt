package com.github.jasync.sql.db.postgresql.messages.backend

class AuthenticationSASLMessage(val supportedSASLMechanisms: List<String>) : AuthenticationMessage()
class AuthenticationSASLContinueMessage(val saslData: String) : AuthenticationMessage()
class AuthenticationSASLFinalMessage(val saslData: String) : AuthenticationMessage()
