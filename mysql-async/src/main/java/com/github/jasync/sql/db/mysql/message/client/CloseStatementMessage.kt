package com.github.jasync.sql.db.mysql.message.client

class CloseStatementMessage(val statementId : ByteArray) : ClientMessage( ClientMessage.PreparedStatementClose )