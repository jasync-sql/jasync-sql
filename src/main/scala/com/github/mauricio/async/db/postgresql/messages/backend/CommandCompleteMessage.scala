package com.github.mauricio.postgresql.messages.backend

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 1:04 AM
 */
case class CommandCompleteMessage ( val rowsAffected : Int, val statusMessage : String )
  extends Message( Message.CommandComplete )