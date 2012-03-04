package com.github.mauricio.postgresql.messages

import com.github.mauricio.postgresql.Message

/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 8:31 PM
 */

class QueryMessage( val query : String ) extends Message( Message.Query, query )