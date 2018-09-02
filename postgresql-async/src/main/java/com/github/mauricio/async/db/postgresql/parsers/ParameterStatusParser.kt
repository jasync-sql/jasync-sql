package com.github.mauricio.async.db.postgresql.parsers

import com.github.jasync.sql.db.util.ByteBufferUtils.readCString
import com.github.mauricio.async.db.postgresql.messages.backend.ParameterStatusMessage
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset


class ParameterStatusParser(val charset: Charset) : MessageParser {

  override fun parseMessage(b: ByteBuf): ServerMessage {
    return ParameterStatusMessage(readCString(b, charset), readCString(b, charset))
  }

}