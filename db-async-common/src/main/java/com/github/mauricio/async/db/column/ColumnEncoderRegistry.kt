
package com.github.mauricio.async.db.column

interface ColumnEncoderRegistry {

  fun encode( value : Any ) : String

  fun kindOf( value : Any ) : Int

}