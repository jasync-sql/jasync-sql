package com.github.mauricio.postgresql.messages

import com.github.mauricio.postgresql.{FrontendMessage, Message}


/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 8:31 PM
 */

class QueryMessage( val query : String ) extends FrontendMessage( Message.Query )