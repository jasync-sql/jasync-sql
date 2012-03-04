package com.github.mauricio.postgresql

object Message {

  val Notification = 'A'
  val CommandComplete = 'C'
  val Close = 'X'
  val DataRow = 'D'
  val Error = 'E'
  val EmptyQuery = 'I'
  val BackendKeyData = 'K'
  val Notice = 'N'
  val NoData = 'n'
  val Query = 'Q'
  val AuthenticationOk = 'R'
  val ParameterStatus = 'S'
  val PortalSuspended = 's'
  val RowDescription = 'T'
  val ReadyForQuery = 'Z'
  val ParseComplete = '1'
  val BindComplete = '2'

}

class Message ( val name : Char, val content : Any ) {

  override def hashCode : Int  = {
    "%s-%s".format( this.name, this.content ).hashCode()
  }

  override def equals( other : Any ) : Boolean = {

    other match {
      case o : Message => {
        this.name.equals(o.name) && this.content.equals( o.content )
      }
      case _ => false
    }

  }

}