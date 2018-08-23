
package com.github.mauricio.async.db.column

interface ColumnEncoder {

  fun encode(value: Any): String = value.toString()

}
