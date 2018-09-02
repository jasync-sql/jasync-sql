package com.github.mauricio.async.db.postgresql.encoders

import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import com.github.jasync.sql.db.util.length
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import com.github.mauricio.async.db.util.Log
import com.github.mauricio.async.db.util.ByteBufferUtils
import com.github.mauricio.async.db.column.ColumnEncoderRegistry
import java.nio.charset.Charset
import io.netty.buffer.Unpooled
import io.netty.buffer.ByteBuf
import mu.KotlinLogging
import scala.collection.mutable.ArrayBuffer

private val logger = KotlinLogging.logger {}

interface PreparedStatementEncoderHelper {

  fun writeExecutePortal(
      statementIdBytes: Array<Byte>,
      query: String,
      values: List<Any>,
      encoder: ColumnEncoderRegistry,
      charset: Charset,
      writeDescribe: Boolean = false
  ): ByteBuf {

    logger.debug("Preparing execute portal to statement ($query) - values (${values.mkString(", ")}) - $charset")

    val bindBuffer = Unpooled.buffer(1024)

    bindBuffer.writeByte(ServerMessage.Bind)
    bindBuffer.writeInt(0)

    bindBuffer.writeBytes(statementIdBytes)
    bindBuffer.writeByte(0)
    bindBuffer.writeBytes(statementIdBytes)
    bindBuffer.writeByte(0)

    bindBuffer.writeShort(0)

    bindBuffer.writeShort(values.length)

    val decodedValues = if (logger.isDebugEnabled) {
      ArrayBuffer<String>(values.size)
    } else {
      null
    }

    for (value < -values) {
      if (isNull(value)) {
        bindBuffer.writeInt(-1)

        if (log.isDebugEnabled) {
          decodedValues += null
        }
      } else {
        val encodedValue = encoder.encode(value)

        if (log.isDebugEnabled) {
          decodedValues += encodedValue
        }

        if (isNull(encodedValue)) {
          bindBuffer.writeInt(-1)
        } else {
          val content = encodedValue.getBytes(charset)
          bindBuffer.writeInt(content.length)
          bindBuffer.writeBytes(content)
        }

      }
    }

    if (log.isDebugEnabled) {
      log.debug(s"Executing portal - statement id (${statementIdBytes.mkString("-")}) - statement ($query) - encoded values (${decodedValues.mkString(", ")}) - original values (${values.mkString(", ")})")
    }

    bindBuffer.writeShort(0)

    ByteBufferUtils.writeLength(bindBuffer)

    if (writeDescribe) {
      val describeLength = 1 + 4 + 1 + statementIdBytes.length + 1
      val describeBuffer = bindBuffer
      describeBuffer.writeByte(ServerMessage.Describe)
      describeBuffer.writeInt(describeLength - 1)
      describeBuffer.writeByte('P')
      describeBuffer.writeBytes(statementIdBytes)
      describeBuffer.writeByte(0)
    }

    val executeLength = 1 + 4 + statementIdBytes.length + 1 + 4
    val executeBuffer = Unpooled.buffer(executeLength)
    executeBuffer.writeByte(ServerMessage.Execute)
    executeBuffer.writeInt(executeLength - 1)
    executeBuffer.writeBytes(statementIdBytes)
    executeBuffer.writeByte(0)
    executeBuffer.writeInt(0)

    val closeLength = 1 + 4 + 1 + statementIdBytes.length + 1
    val closeBuffer = Unpooled.buffer(closeLength)
    closeBuffer.writeByte(ServerMessage.CloseStatementOrPortal)
    closeBuffer.writeInt(closeLength - 1)
    closeBuffer.writeByte('P')
    closeBuffer.writeBytes(statementIdBytes)
    closeBuffer.writeByte(0)

    val syncBuffer = Unpooled.buffer(5)
    syncBuffer.writeByte(ServerMessage.Sync)
    syncBuffer.writeInt(4)

    Unpooled.wrappedBuffer(bindBuffer, executeBuffer, syncBuffer, closeBuffer)

  }

  fun isNull(value: Any): Boolean = value == null || value == None

}