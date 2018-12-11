package com.github.jasync.sql.db.postgresql.messages.frontend

class StartupMessage(val parameters: List<Pair<String, Any>>) : InitialClientMessage
