package com.github.jasync.sql.db.postgresql.codec

import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import com.github.jasync.sql.db.exceptions.EncoderNotAvailableException
import com.github.jasync.sql.db.util.BufferDumper
import com.github.jasync.sql.db.postgresql.encoders.CloseMessageEncoder
import com.github.jasync.sql.db.postgresql.encoders.CredentialEncoder
import com.github.jasync.sql.db.postgresql.encoders.ExecutePreparedStatementEncoder
import com.github.jasync.sql.db.postgresql.encoders.PreparedStatementOpeningEncoder
import com.github.jasync.sql.db.postgresql.encoders.QueryMessageEncoder
import com.github.jasync.sql.db.postgresql.encoders.SSLMessageEncoder
import com.github.jasync.sql.db.postgresql.encoders.StartupMessageEncoder
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.ClientMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.SSLRequestMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.StartupMessage
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder
import mu.KotlinLogging
import java.nio.charset.Charset

private val logger = KotlinLogging.logger {}


class MessageEncoder(charset: Charset, encoderRegistry: ColumnEncoderRegistry) : MessageToMessageEncoder<Any>() {
  private val executeEncoder = ExecutePreparedStatementEncoder(charset, encoderRegistry)
  private val openEncoder = PreparedStatementOpeningEncoder(charset, encoderRegistry)
  private val startupEncoder = StartupMessageEncoder(charset)
  private val queryEncoder = QueryMessageEncoder(charset)
  private val credentialEncoder = CredentialEncoder(charset)

  override fun encode(ctx: ChannelHandlerContext, msg: Any, out: MutableList<Any>) {
    val buffer = when (msg) {
      is SSLRequestMessage -> SSLMessageEncoder.encode()
      is StartupMessage -> startupEncoder.encode(msg)
      is ClientMessage -> {
        val encoder = when (msg.kind) {
          ServerMessage.Close -> CloseMessageEncoder
          ServerMessage.Execute -> this.executeEncoder
          ServerMessage.Parse -> this.openEncoder
          ServerMessage.Query -> this.queryEncoder
          ServerMessage.PasswordMessage -> this.credentialEncoder
          else -> throw  EncoderNotAvailableException(msg)
        }
        encoder.encode(msg)
      }
      else -> throw  IllegalArgumentException("Can not encode message %s".format(msg))
    }
    logger.trace { "Sending message ${msg.javaClass.name}\n${BufferDumper.dumpAsHex(buffer)}" }
    out.add(buffer)
  }

}
