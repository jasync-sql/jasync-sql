package com.github.mauricio.async.db.postgresql.parsers

import com.github.jasync.sql.db.util.ByteBufferUtils
import com.github.mauricio.async.db.postgresql.messages.backend.PostgreSQLColumnData
import com.github.mauricio.async.db.postgresql.messages.backend.RowDescriptionMessage
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

/**

RowDescription (B)
Byte1('T')
Identifies the message as a row description.

Int32
Length of message contents in bytes, including self.

Int16
Specifies the number of fields in a row (can be zero).

Then, for each field, there is the following:

String
The field name.

Int32
If the field can be identified as a column of a specific table, the object ID of the table; otherwise zero.

Int16
If the field can be identified as a column of a specific table, the attribute number of the column; otherwise zero.

Int32
The object ID of the field's data type.

Int16
The data type size (see pg_type.typlen). Note that negative values denote variable-width types.

Int32
The type modifier (see pg_attribute.atttypmod). The meaning of the modifier is type-specific.

Int16
The format code being used for the field. Currently will be zero (text) or one (binary). In a RowDescription returned from the statement variant of Describe, the format code is not yet known and will always be zero.
 *
 */


class RowDescriptionParser(val charset: Charset) : MessageParser {

  override fun parseMessage(b: ByteBuf): ServerMessage {

    val columnsCount = b.readShort()
    val columns = mutableListOf<PostgreSQLColumnData>()

    0.until(columnsCount).forEach {
      columns.add(
          PostgreSQLColumnData(
              name = ByteBufferUtils.readCString(b, charset),
              tableObjectId = b.readInt(),
              columnNumber = b.readShort().toInt(),
              dataType = b.readInt(),
              dataTypeSize = b.readShort().toLong(),
              dataTypeModifier = b.readInt(),
              fieldFormat = b.readShort().toInt()
          )
      )
    }

    return RowDescriptionMessage(columns.toTypedArray())
  }

}