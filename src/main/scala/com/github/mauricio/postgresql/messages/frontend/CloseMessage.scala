package com.github.mauricio.postgresql.messages.frontend

import com.github.mauricio.postgresql.messages.backend.Message


/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 8:39 PM
 */

object CloseMessage {

  val Instance = new CloseMessage()

}

class CloseMessage extends FrontendMessage( Message.Close )
