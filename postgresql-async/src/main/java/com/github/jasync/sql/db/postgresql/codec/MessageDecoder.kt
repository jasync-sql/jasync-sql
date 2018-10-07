package com.github.jasync.sql.db.postgresql.codec

import com.github.jasync.sql.db.exceptions.NegativeMessageSizeException
import com.github.jasync.sql.db.postgresql.exceptions.MessageTooLongException
import com.github.jasync.sql.db.postgresql.messages.backend.SSLResponseMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import com.github.jasync.sql.db.postgresql.parsers.AuthenticationStartupParser
import com.github.jasync.sql.db.postgresql.parsers.MessageParsersRegistry
import com.github.jasync.sql.db.util.BufferDumper
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import mu.KotlinLogging
import java.nio.charset.Charset

private val logger = KotlinLogging.logger {}
const val MessageDecoder_DefaultMaximumSize = 16777216

class MessageDecoder(private val sslEnabled: Boolean, val charset: Charset, private val maximumMessageSize: Int = MessageDecoder_DefaultMaximumSize) : ByteToMessageDecoder() {

  private val parser = MessageParsersRegistry(charset)

  private var sslChecked = false

  public override fun decode(ctx: ChannelHandlerContext, b: ByteBuf, out: MutableList<Any>) {

    if (sslEnabled && !sslChecked) {
      val code = b.readByte()
      sslChecked = true
      out.add(SSLResponseMessage(code.toChar() == 'S'))
    } else if (b.readableBytes() >= 5) {

      b.markReaderIndex()

      val code = b.readByte()
      val lengthWithSelf = b.readInt()
      val length = lengthWithSelf - 4

      if (length < 0) {
        throw NegativeMessageSizeException(code, length)
      }

      if (length > maximumMessageSize) {
        throw MessageTooLongException(code, length, maximumMessageSize)
      }

      if (b.readableBytes() >= length) {

        logger.trace{"Received buffer ${code.toChar()}($code)\n${BufferDumper.dumpAsHex(b)}"}

        val result = when (code.toInt()) {
          ServerMessage.Authentication -> AuthenticationStartupParser.parseMessage(b)
          else -> parser.parse(code.toInt(), b.readSlice(length))
        }
        out.add(result)
      } else {
        b.resetReaderIndex()
        return
      }
    }

  }

}
