/*
 * Copyright 2013 Maurício Linhares
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

package com.github.mauricio.postgresql.column

import com.github.mauricio.postgresql.exceptions.DateEncoderNotAvailableException
import java.sql.Timestamp
import java.util.{Calendar, Date}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{ReadableDateTime, DateTime}

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
