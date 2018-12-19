package com.github.jasync.sql.db.column

import com.github.jasync.sql.db.general.ColumnData
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

interface ColumnDecoderRegistry {

    fun decode(kind: ColumnData, value: ByteBuf, charset: Charset): Any

}
