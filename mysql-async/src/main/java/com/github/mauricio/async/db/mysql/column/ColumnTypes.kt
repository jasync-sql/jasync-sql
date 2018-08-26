
package com.github.mauricio.async.db.mysql.column

object ColumnTypes {

  val FIELD_TYPE_BIT = 16

  val FIELD_TYPE_BLOB = 252

  val FIELD_TYPE_DATE = 10

  val FIELD_TYPE_DATETIME = 12

  val FIELD_TYPE_DECIMAL = 0

  val FIELD_TYPE_NUMERIC = -10

  val FIELD_TYPE_DOUBLE = 5

  val FIELD_TYPE_ENUM = 247

  val FIELD_TYPE_FLOAT = 4

  val FIELD_TYPE_GEOMETRY = 255

  val FIELD_TYPE_INT24 = 9

  val FIELD_TYPE_LONG = 3

  val FIELD_TYPE_LONG_BLOB = 251

  val FIELD_TYPE_LONGLONG = 8

  val FIELD_TYPE_MEDIUM_BLOB = 250

  val FIELD_TYPE_NEW_DECIMAL = 246

  val FIELD_TYPE_NEWDATE = 14

  val FIELD_TYPE_NULL = 6

  val FIELD_TYPE_SET = 248

  val FIELD_TYPE_SHORT = 2

  val FIELD_TYPE_STRING = 254

  val FIELD_TYPE_TIME = 11

  val FIELD_TYPE_TIMESTAMP = 7

  val FIELD_TYPE_TINY = 1

  val FIELD_TYPE_TINY_BLOB = 249

  val FIELD_TYPE_VAR_STRING = 253

  val FIELD_TYPE_VARCHAR = 15

  val FIELD_TYPE_YEAR = 13

  val Mapping = mapOf(
    FIELD_TYPE_BIT to "bit",
    FIELD_TYPE_BLOB to "blob",
    FIELD_TYPE_DATE to "date",
    FIELD_TYPE_DATETIME to "datetime",
    FIELD_TYPE_DECIMAL to "decimal",
    FIELD_TYPE_DOUBLE to "double",
    FIELD_TYPE_ENUM to "enum",
    FIELD_TYPE_FLOAT to "float",
    FIELD_TYPE_GEOMETRY to "geometry",
    FIELD_TYPE_INT24 to "int64",
    FIELD_TYPE_LONG to "integer",
    FIELD_TYPE_LONGLONG to "long",
    FIELD_TYPE_LONG_BLOB to "long_blob",
    FIELD_TYPE_MEDIUM_BLOB to "medium_blob",
    FIELD_TYPE_NEW_DECIMAL to "new_decimal",
    FIELD_TYPE_NEWDATE to "new_date",
    FIELD_TYPE_NULL to "null",
    FIELD_TYPE_NUMERIC to "numeric",
    FIELD_TYPE_SET to "set",
    FIELD_TYPE_SHORT to "short",
    FIELD_TYPE_STRING to "string",
    FIELD_TYPE_TIME to "time",
    FIELD_TYPE_TIMESTAMP to "timestamp",
    FIELD_TYPE_TINY to "tiny",
    FIELD_TYPE_TINY_BLOB to "tiny_blob",
    FIELD_TYPE_VAR_STRING to "var_string",
    FIELD_TYPE_VARCHAR to "varchar",
    FIELD_TYPE_YEAR to "year"
  )

}
