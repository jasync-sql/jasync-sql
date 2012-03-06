package com.github.mauricio.postgresql

/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 4:01 PM
 */

class QueryResult ( val rowsAffected : Int, val statusMessage : String, val rows : Option[Query] ) {

  override def toString : String = {
    "QueryResult{rows -> %s,status -> %s}".format( this.rowsAffected, this.statusMessage )
  }

}
