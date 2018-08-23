
package com.github.mauricio.async.db.general

interface ColumnData {

  fun name (): String
  fun dataType (): Int
  fun dataTypeSize (): Long

}