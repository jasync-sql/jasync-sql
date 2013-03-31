package com.github.mauricio.postgresql.messages.frontend

import com.github.mauricio.postgresql.messages.backend.Message

/**
 * User: Maurício Linhares
 * Date: 3/12/12
 * Time: 6:50 PM
 */

class PreparedStatementExecuteMessage( query : String, values : Seq[Any] )
  extends PreparedStatementMessage(Message.Execute, query, values)
