package com.github.mauricio.postgresql.column

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 6:12 PM
 */

object DateDecoder extends ColumnDecoder {

  private val parser = DateTimeFormat.forPattern("yyyy-MM-dd")

  def decode(value: String): LocalDate = {
    this.parser.parseLocalDate(value)
  }
}
