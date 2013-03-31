package com.github.mauricio.postgresql.messages.frontend

import com.github.mauricio.postgresql.messages.backend.Message


/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 7:34 PM
 */

class StartupMessage ( val parameters : List[(String, Any)] ) extends FrontendMessage(Message.Startup)
