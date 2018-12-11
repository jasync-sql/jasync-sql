package com.github.jasync.sql.db.postgresql.column

import com.github.jasync.sql.db.column.ColumnEncoderDecoder
import com.github.jasync.sql.db.exceptions.DateEncoderNotAvailableException
import mu.KotlinLogging
import org.joda.time.Period
import org.joda.time.ReadableDuration
import org.joda.time.ReadablePeriod
import org.joda.time.format.ISOPeriodFormat
import org.joda.time.format.PeriodFormatterBuilder

private val logger = KotlinLogging.logger {}

object PostgreSQLIntervalEncoderDecoder : ColumnEncoderDecoder {

    /* Postgres accepts all ISO8601 formats. */
    private val formatter = ISOPeriodFormat.standard()

    override fun encode(value: Any): String {
        return when (value) {
            is ReadablePeriod -> formatter.print(value)
            is ReadableDuration -> value.toString() // funaults to ISO8601
            else -> throw DateEncoderNotAvailableException(value)
        }
    }

    /* these should only be used for parsing: */
    private fun postgresYMDBuilder(builder: PeriodFormatterBuilder) = builder
        .appendYears().appendSuffix(" year", " years").appendSeparator(" ")
        .appendMonths().appendSuffix(" mon", " mons").appendSeparator(" ")
        .appendDays().appendSuffix(" day", " days").appendSeparator(" ")

    private val postgres_verboseParser =
        postgresYMDBuilder(PeriodFormatterBuilder().appendLiteral("@ "))
            .appendHours().appendSuffix(" hour", " hours").appendSeparator(" ")
            .appendMinutes().appendSuffix(" min", " mins").appendSeparator(" ")
            .appendSecondsWithOptionalMillis().appendSuffix(" sec", " secs")
            .toFormatter()

    private fun postgresHMSBuilder(builder: PeriodFormatterBuilder) = builder
        // .printZeroAlways() // really all-or-nothing
        .rejectSignedValues(true) // XXX: sign should apply to all
        .appendHours().appendSuffix(":")
        .appendMinutes().appendSuffix(":")
        .appendSecondsWithOptionalMillis()

    private val hmsParser =
        postgresHMSBuilder(PeriodFormatterBuilder())
            .toFormatter()

    private val postgresParser =
        postgresHMSBuilder(postgresYMDBuilder(PeriodFormatterBuilder()))
            .toFormatter()

    /* These sql_standard parsers don't handle negative signs correctly. */
    private fun sqlDTBuilder(builder: PeriodFormatterBuilder) =
        postgresHMSBuilder(
            builder
                .appendDays().appendSeparator(" ")
        )

    private val sqlDTParser =
        sqlDTBuilder(PeriodFormatterBuilder())
            .toFormatter()

    private val sqlParser =
        sqlDTBuilder(
            PeriodFormatterBuilder()
                .printZeroAlways()
                .rejectSignedValues(true) // XXX: sign should apply to both
                .appendYears().appendSeparator("-").appendMonths()
                .rejectSignedValues(false)
                .printZeroNever()
                .appendSeparator(" ")
        )
            .toFormatter()

    /* This supports all positive intervals, and intervalstyle of postgres_verbose, and iso_8601 perfectly.
     * If intervalstyle is set to postgres or sql_standard, some negative intervals may be rejected.
     */
    override fun decode(value: String): Period {
        return if (value.isEmpty()) { /* huh? */
            Period.ZERO
        } else {
            val format = (
                    if (value.startsWith('P')) /* iso_8601 */
                        formatter
                    else if (value.startsWith("@ "))
                        postgres_verboseParser
                    else {
                        /* try to guess based on what comes after the first number */
                        val i = value.indexOfFirst { !it.isDigit() }.let { if ("-+".contains(value[0])) 1 else 0 }
                        if (i < 0 || ":.".contains(value[i])) /* simple HMS (to support group negation) */
                            hmsParser
                        else if (value[i] == '-') /* sql_standard: Y-M */
                            sqlParser
                        else if (value[i] == ' ' && i + 1 < value.length && value[i + 1].isDigit()) /* sql_standard: D H:M:S */
                            sqlDTParser
                        else
                            postgresParser
                    }
                    )
            if ((format == hmsParser) && value.startsWith('-'))
                format.parsePeriod(value.substring(1)).negated()
            else if (value.endsWith(" ago")) /* only really applies to postgres_verbose, but shouldn't hurt */
                format.parsePeriod(value.removeSuffix(" ago")).negated()
            else
                format.parsePeriod(value)
        }
    }
}
