package com.github.mauricio.postgresql.messages.backend

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 1:13 AM
 */
case class ParameterStatusMessage ( val key : String, val value : String )
  extends Message( Message.ParameterStatus )