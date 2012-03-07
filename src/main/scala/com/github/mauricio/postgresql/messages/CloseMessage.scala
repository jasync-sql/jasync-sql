package com.github.mauricio.postgresql.messages

import com.github.mauricio.postgresql.{FrontendMessage, Message}


/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 8:39 PM
 */

object CloseMessage {

  val Instance = new CloseMessage()

}

class CloseMessage extends FrontendMessage( Message.Close )
