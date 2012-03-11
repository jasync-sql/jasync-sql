package com.github.mauricio.postgresql.messages

import com.github.mauricio.postgresql.{Message, FrontendMessage}


/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 7:34 PM
 */

class StartupMessage ( val parameters : List[(String, String)] ) extends FrontendMessage(Message.Startup)
