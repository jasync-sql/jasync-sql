package com.github.mauricio.postgresql.column

import org.joda.time.format.DateTimeFormat
import org.joda.time.{ReadableDateTime, DateTime}
import com.github.mauricio.postgresql.exceptions.DateEncoderNotAvailableException
import java.util.{Calendar, Date}
import java.sql.Timestamp

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 6:10 PM
 */

object TimestampEncoderDecoder {
  val Instance = new TimestampEncoderDecoder()
}

class TimestampEncoderDecoder extends ColumnEncoderDecoder {

  private val format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

  def formatter = format

  override def decode(value: String): ReadableDateTime = {
    formatter.parseDateTime(value)
  }

  override def encode( value : Any ) : String = {
    value match {
      case t : Timestamp => this.formatter.print( new DateTime(t) )
      case t : Date => this.formatter.print( new DateTime(t) )
      case t : Calendar => this.formatter.print( new DateTime(t) )
      case t : ReadableDateTime => this.formatter.print(t)
      case _ => throw new DateEncoderNotAvailableException(value)
    }
  }

}
