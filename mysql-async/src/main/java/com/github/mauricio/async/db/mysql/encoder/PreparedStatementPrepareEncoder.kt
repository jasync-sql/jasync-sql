
package com.github.mauricio.async.db.mysql.encoder

import com.github.jasync.sql.db.util.ByteBufferUtils
import com.github.mauricio.async.db.mysql.message.client.ClientMessage
import com.github.mauricio.async.db.mysql.message.client.PreparedStatementPrepareMessage
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class PreparedStatementPrepareEncoder( val charset : Charset ) : MessageEncoder {

  override fun encode(message: ClientMessage): ByteBuf {
    val m = message as PreparedStatementPrepareMessage
    val statement = m.statement.toByteArray(charset)
    val buffer = ByteBufferUtils.packetBuffer( 4 + 1 + statement.size)
    buffer.writeByte( m.kind )
    buffer.writeBytes( statement )

    return buffer
  }

}
