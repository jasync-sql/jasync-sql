
package com.github.jasync.sql.db.mysql.binary.decoder

import io.netty.buffer.ByteBuf
import org.joda.time.LocalDate

object DateDecoder : BinaryDecoder {
  override fun decode(buffer: ByteBuf): LocalDate? {
    val result = TimestampDecoder.decode(buffer)

    return result?.toLocalDate()
  }
}
