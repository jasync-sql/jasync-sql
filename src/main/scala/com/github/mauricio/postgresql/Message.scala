package com.github.mauricio.postgresql

object Message {

  val AuthenticationOk = 'R'
  val ParameterStatus = 'S'
  val BackendKeyData = 'K'
  val CommandComplete = 'C'
  val ReadyForQuery = 'Z'
  val RowDescription = 'T'
  val DataRow = 'D'
  val Error = 'R'
  val Notice = 'N'
  val ParseComplete = '1'
  val BindComplete = '2'
  val Notification = 'A'
  val NoData = 'n'
  val EmptyQuery = 'I'
  val PortalSuspended = 's'

}

class Message ( val name : Char, val content : Any )