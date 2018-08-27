
package com.github.jasync.sql.db.mysql.message.client

data class PreparedStatementPrepareMessage( val statement : String )
  : ClientMessage( ClientMessage.PreparedStatementPrepare )
