package com.github.mauricio.postgresql.messages.backend

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 1:26 AM
 */
class ReadyForQueryMessage ( transactionStatus : Char ) extends Message ( Message.ReadyForQuery )
