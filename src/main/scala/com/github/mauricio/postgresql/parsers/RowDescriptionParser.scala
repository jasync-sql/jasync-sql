package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.{ChannelUtils, Message}

/**
 * User: Maurício Linhares
 * Date: 3/1/12
 * Time: 1:56 AM
 *
 * RowDescription (B)
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


object RowDescriptionParser extends MessageParser {

  import ChannelUtils._

  override def parseMessage(b: ChannelBuffer): Message = {

    val columnsCount = b.readShort()
    val columns = new Array[ColumnData](columnsCount)

    0.until( columnsCount ).foreach {
      index =>
        columns(index) = new ColumnData(
          name = readCString( b ),
          tableObjectId =  b.readInt(),
          columnNumber = b.readShort(),
          dataType = b.readInt(),
          dataTypeSize = b.readShort(),
          dataTypeModifier = b.readInt(),
          fieldFormat = b.readShort()
        )
    }

    new Message( Message.RowDescription, columns )
  }

}
