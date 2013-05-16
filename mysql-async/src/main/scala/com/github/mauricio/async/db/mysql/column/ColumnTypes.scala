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

package com.github.mauricio.async.db.mysql.column

object ColumnTypes {

  final val FIELD_TYPE_BIT = 16

  final val FIELD_TYPE_BLOB = 252

  final val FIELD_TYPE_DATE = 10

  final val FIELD_TYPE_DATETIME = 12

  final val FIELD_TYPE_DECIMAL = 0

  final val FIELD_TYPE_NUMERIC = -10

  final val FIELD_TYPE_DOUBLE = 5

  final val FIELD_TYPE_ENUM = 247

  final val FIELD_TYPE_FLOAT = 4

  final val FIELD_TYPE_GEOMETRY = 255

  final val FIELD_TYPE_INT24 = 9

  final val FIELD_TYPE_LONG = 3

  final val FIELD_TYPE_LONG_BLOB = 251

  final val FIELD_TYPE_LONGLONG = 8

  final val FIELD_TYPE_MEDIUM_BLOB = 250

  final val FIELD_TYPE_NEW_DECIMAL = 246

  final val FIELD_TYPE_NEWDATE = 14

  final val FIELD_TYPE_NULL = 6

  final val FIELD_TYPE_SET = 248

  final val FIELD_TYPE_SHORT = 2

  final val FIELD_TYPE_STRING = 254

  final val FIELD_TYPE_TIME = 11

  final val FIELD_TYPE_TIMESTAMP = 7

  final val FIELD_TYPE_TINY = 1

  final val FIELD_TYPE_TINY_BLOB = 249

  final val FIELD_TYPE_VAR_STRING = 253

  final val FIELD_TYPE_VARCHAR = 15

  final val FIELD_TYPE_YEAR = 13

  val Mapping = Map(
    FIELD_TYPE_BIT -> "bit",
    FIELD_TYPE_BLOB -> "blob",
    FIELD_TYPE_DATE -> "date",
    FIELD_TYPE_DATETIME -> "datetime",
    FIELD_TYPE_DECIMAL -> "decimal",
    FIELD_TYPE_DOUBLE -> "double",
    FIELD_TYPE_ENUM -> "enum",
    FIELD_TYPE_FLOAT -> "float",
    FIELD_TYPE_GEOMETRY -> "geometry",
    FIELD_TYPE_INT24 -> "int64",
    FIELD_TYPE_LONG -> "integer",
    FIELD_TYPE_LONGLONG -> "long",
    FIELD_TYPE_LONG_BLOB -> "long_blob",
    FIELD_TYPE_MEDIUM_BLOB -> "medium_blob",
    FIELD_TYPE_NEW_DECIMAL -> "new_decimal",
    FIELD_TYPE_NEWDATE -> "new_date",
    FIELD_TYPE_NULL -> "null",
    FIELD_TYPE_NUMERIC -> "numeric",
    FIELD_TYPE_SET -> "set",
    FIELD_TYPE_SHORT -> "short",
    FIELD_TYPE_STRING -> "string",
    FIELD_TYPE_TIME -> "time",
    FIELD_TYPE_TIMESTAMP -> "timestamp",
    FIELD_TYPE_TINY -> "tiny",
    FIELD_TYPE_TINY_BLOB -> "tiny_blob",
    FIELD_TYPE_VAR_STRING -> "var_string",
    FIELD_TYPE_VARCHAR -> "varchar",
    FIELD_TYPE_YEAR -> "year"
  )

}
