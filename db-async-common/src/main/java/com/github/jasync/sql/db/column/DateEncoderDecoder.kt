package com.github.jasync.sql.db.column

import com.github.jasync.sql.db.exceptions.DateEncoderNotAvailableException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

object DateEncoderDecoder : ColumnEncoderDecoder {

    private const val ZeroedDate = "0000-00-00"

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd") // DateTimeFormat.forPattern("yyyy-MM-dd")

    override fun decode(value: String): LocalDate? =
        if (ZeroedDate == value) {
            null
        } else {
            LocalDate.parse(value, this.formatter)
        }

    override fun encode(value: Any): String {
        return when (value) {
            is java.sql.Date -> value.toLocalDate().format(this.formatter)
            is TemporalAccessor -> this.formatter.format(value)
            else -> throw DateEncoderNotAvailableException(value)
        }
    }

}
