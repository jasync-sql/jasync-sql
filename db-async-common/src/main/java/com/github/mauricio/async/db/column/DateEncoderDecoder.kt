
package com.github.mauricio.async.db.column

import org.joda.time.format.DateTimeFormat
import org.joda.time.ReadablePartial
import org.joda.time.LocalDate
import com.github.mauricio.async.db.exceptions.DateEncoderNotAvailableException

object DateEncoderDecoder : ColumnEncoderDecoder {

  private val ZeroedDate = "0000-00-00"

  private val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")

  override fun decode(value: String): LocalDate? =
    if ( ZeroedDate == value ) {
      null
    } else {
      this.formatter.parseLocalDate(value)
    }

  override fun encode(value: Any): String {
    return when(value) {
      is java.sql.Date -> this.formatter.print(LocalDate(value))
      is ReadablePartial -> this.formatter.print(value)
      else -> throw DateEncoderNotAvailableException(value)
    }
  }

}
