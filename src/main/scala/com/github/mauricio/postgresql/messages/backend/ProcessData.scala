package com.github.mauricio.postgresql.messages.backend

/**
 * User: Maurício Linhares
 * Date: 2/28/12
 * Time: 11:13 PM
 */

case class ProcessData ( val processId : Int, val secretKey : Int )
  extends Message( Message.BackendKeyData )
