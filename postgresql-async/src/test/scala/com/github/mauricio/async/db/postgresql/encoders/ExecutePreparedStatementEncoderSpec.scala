package com.github.mauricio.async.db.postgresql.encoders

import com.github.mauricio.async.db.postgresql.column.PostgreSQLColumnEncoderRegistry
import com.github.mauricio.async.db.postgresql.messages.frontend.PreparedStatementExecuteMessage
import io.netty.util.CharsetUtil
import org.specs2.mutable.Specification

class ExecutePreparedStatementEncoderSpec extends Specification {

  val registry = new PostgreSQLColumnEncoderRegistry()
  val encoder = new ExecutePreparedStatementEncoder(CharsetUtil.UTF_8, registry)
  val sampleMessage = Array[Byte](66,0,0,0,18,49,0,49,0,0,0,0,1,-1,-1,-1,-1,0,0,69,0,0,0,10,49,0,0,0,0,0,83,0,0,0,4,67,0,0,0,7,80,49,0)

  "encoder" should {

    "correctly handle the case where an encoder returns null" in {

      val message = new PreparedStatementExecuteMessage(1, "select * from users", List(Some(null)), registry)

      val result = encoder.encode(message)

      val bytes = new Array[Byte](result.readableBytes())
      result.readBytes(bytes)

      bytes === sampleMessage
    }

  }

}
