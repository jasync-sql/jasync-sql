package com.github.mauricio.async.db.mysql.message.client

case class SendLongDataMessage (
                                 statementId : Array[Byte],
                                 value : Any,
                                 paramId : Int )