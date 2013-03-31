package com.github.mauricio.postgresql.messages.frontend

import com.github.mauricio.postgresql.messages.backend.Message


/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 8:31 PM
 */

class QueryMessage( val query : String ) extends FrontendMessage( Message.Query )