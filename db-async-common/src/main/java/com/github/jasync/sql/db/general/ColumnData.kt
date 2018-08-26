
package com.github.jasync.sql.db.general

interface ColumnData {

  val name: String
  fun dataType (): Int
  fun dataTypeSize (): Long

}
