package com.github.mauricio.postgresql.messages.frontend

import com.github.mauricio.postgresql.messages.backend.Message

/**
 * User: Maurício Linhares
 * Date: 3/12/12
 * Time: 1:00 AM
 */

class PreparedStatementOpeningMessage( query : String, values : Seq[Any] )
  extends PreparedStatementMessage(Message.Parse, query, values)