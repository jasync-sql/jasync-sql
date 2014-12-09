package com.github.mauricio.async.db.mysql.message.client

case class SendLongDataMessage (
                                 statementId : Array[Byte],
                                 value : Any,
                                 paramId : Int )
  extends ClientMessage( ClientMessage.PreparedStatementSendLongData ) {

  override def toString = "SendLongDataMessage(statementId=" + statementId + ",paramId=" + paramId + ",value.getClass=" + value.getClass.getName +")"
}