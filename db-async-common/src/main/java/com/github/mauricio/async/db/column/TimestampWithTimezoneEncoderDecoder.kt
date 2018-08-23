
package com.github.mauricio.async.db.column

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object TimestampWithTimezoneEncoderDecoder : TimestampEncoderDecoder() {

  private val format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ")

  override fun formatter ()= format

  override fun decode(value: String): Any {
    return formatter().parseDateTime(value)
  }

}
