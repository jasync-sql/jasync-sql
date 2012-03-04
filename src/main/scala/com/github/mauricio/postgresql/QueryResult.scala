package com.github.mauricio.postgresql

/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 4:01 PM
 */

class QueryResult ( val rowsAffected : Int ) {

  override def toString : String = {
    "QueryResult{rows -> %s}".format( rowsAffected )
  }

}
