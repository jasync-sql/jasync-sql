
package com.github.jasync.sql.db.general

interface ColumnData {

  fun name (): String
  fun dataType (): Int
  fun dataTypeSize (): Long

}
