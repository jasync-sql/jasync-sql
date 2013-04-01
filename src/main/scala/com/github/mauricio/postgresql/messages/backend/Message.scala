package com.github.mauricio.postgresql.messages.backend

object Message {
  val Authentication = 'R'
  val BackendKeyData = 'K'
  val Bind = 'B'
  val BindComplete = '2'
  val CommandComplete = 'C'
  val Close = 'X'
  val CloseStatementOrPortal = 'C'
  val CloseComplete = '3'
  val DataRow = 'D'
  val Describe = 'D'
  val Error = 'E'
  val Execute = 'E'
  val EmptyQuery = 'I'
  val NoData = 'n'
  val Notice = 'N'
  val Notification = 'A'
  val ParameterStatus = 'S'
  val Parse = 'P'
  val ParseComplete = '1'
  val PasswordMessage = 'p'
  val PortalSuspended = 's'
  val Query = 'Q'
  val RowDescription = 'T'
  val ReadyForQuery = 'Z'
  val Startup : Char = 0
  val Sync = 'S'
}

class Message ( val name : Char )