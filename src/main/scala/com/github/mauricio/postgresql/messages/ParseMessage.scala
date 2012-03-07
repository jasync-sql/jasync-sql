package com.github.mauricio.postgresql.messages

import com.github.mauricio.postgresql.{Message, FrontendMessage}


/**
 * User: Maurício Linhares
 * Date: 3/7/12
 * Time: 12:11 AM
 */

class ParseMessage( val command : String, val parameterTypes : Array[Int] ) extends FrontendMessage(Message.Parse)