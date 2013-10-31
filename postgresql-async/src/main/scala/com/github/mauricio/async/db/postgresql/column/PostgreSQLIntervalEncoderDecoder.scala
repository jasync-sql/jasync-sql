/*
 * Copyright 2013 Maurício Linhares
 * Copyright 2013 Dylan Simon
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.async.db.postgresql.column

import com.github.mauricio.async.db.column.ColumnEncoderDecoder
import com.github.mauricio.async.db.exceptions.DateEncoderNotAvailableException
import com.github.mauricio.async.db.util.Log
import org.joda.time.{Period, ReadablePeriod, ReadableDuration}
import org.joda.time.format.{ISOPeriodFormat, PeriodFormatterBuilder}

object PostgreSQLIntervalEncoderDecoder extends ColumnEncoderDecoder {

  private val log = Log.getByName(this.getClass.getName)

  /* Postgres accepts all ISO8601 formats. */
  private val formatter = ISOPeriodFormat.standard

  override def encode(value : Any) : String = {
    value match {
      case t : ReadablePeriod => formatter.print(t)
      case t : ReadableDuration => t.toString // defaults to ISO8601
      case _ => throw new DateEncoderNotAvailableException(value)
    }
  }

  /* these should only be used for parsing: */
  private def postgresYMDBuilder(builder : PeriodFormatterBuilder) = builder
    .appendYears  .appendSuffix(" year", " years").appendSeparator(" ")
    .appendMonths .appendSuffix(" mon",  " mons" ).appendSeparator(" ")
    .appendDays   .appendSuffix(" day",  " days" ).appendSeparator(" ")

  private val postgres_verboseParser =
    postgresYMDBuilder(new PeriodFormatterBuilder().appendLiteral("@ "))
    .appendHours  .appendSuffix(" hour", " hours").appendSeparator(" ")
    .appendMinutes.appendSuffix(" min",  " mins" ).appendSeparator(" ")
    .appendSecondsWithOptionalMillis.appendSuffix(" sec", " secs")
    .toFormatter

  private def postgresHMSBuilder(builder : PeriodFormatterBuilder) = builder
    // .printZeroAlways // really all-or-nothing
    .rejectSignedValues(true) // XXX: sign should apply to all
    .appendHours  .appendSeparator(":")
    .appendMinutes.appendSeparator(":")
    .appendSecondsWithOptionalMillis

  private val secsParser = new PeriodFormatterBuilder()
    .appendSecondsWithOptionalMillis
    .toFormatter

  private val postgresParser = 
    postgresHMSBuilder(postgresYMDBuilder(new PeriodFormatterBuilder()))
    .toFormatter

  /* These sql_standard parsers don't handle negative signs correctly. */
  private def sqlDTBuilder(builder : PeriodFormatterBuilder) =
    postgresHMSBuilder(builder
    .appendDays.appendSeparator(" "))

  private val sqlDTParser =
    sqlDTBuilder(new PeriodFormatterBuilder())
    .toFormatter

  private val sqlParser =
    sqlDTBuilder(new PeriodFormatterBuilder()
    .printZeroAlways
    .rejectSignedValues(true) // XXX: sign should apply to both
    .appendYears.appendSeparator("-").appendMonths
    .rejectSignedValues(false)
    .printZeroNever
    .appendSeparator(" "))
    .toFormatter

  /* This supports all positive intervals, and intervalstyle of postgres_verbose, and iso_8601 perfectly.
   * If intervalstyle is set to postgres or sql_standard, some negative intervals may be rejected.
   */
  def decode(value : String) : Period = {
    if (value.isEmpty) /* huh? */
      Period.ZERO
    else {
      val format = (
        if (value(0).equals('P')) /* iso_8601 */
          formatter
        else if (value.startsWith("@ "))
          postgres_verboseParser
        else {
          /* try to guess based on what comes after the first number */
          val i = value.indexWhere(!_.isDigit, if ("-+".contains(value(0))) 1 else 0)
          if (i <= 0 || value(i).equals('.')) /* just a number */
            secsParser /* postgres treats this as seconds */
          else if (value(i).equals('-')) /* sql_standard: Y-M */
            sqlParser
          else if (value(i).equals(' ') && i+1 < value.length && value(i+1).isDigit) /* sql_standard: D H:M:S */
            sqlDTParser
          else
            postgresParser
        }
      )
      if (value.endsWith(" ago")) /* only really applies to postgres_verbose, but shouldn't hurt */
        format.parsePeriod(value.stripSuffix(" ago")).negated
      else
        format.parsePeriod(value)
    }
  }
}
