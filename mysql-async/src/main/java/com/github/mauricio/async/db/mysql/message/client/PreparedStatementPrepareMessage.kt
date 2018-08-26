
package com.github.mauricio.async.db.mysql.message.client

data class PreparedStatementPrepareMessage( val statement : String )
  : ClientMessage( ClientMessage.PreparedStatementPrepare )
