package com.github.mauricio.postgresql

/**
 * User: mauricio
 * Date: 3/25/13
 * Time: 10:24 PM
 */
trait ResultSet extends IndexedSeq[Array[Any]] {

  def apply( name : String, row : Int ) : Any

  def apply( column : Int,  row : Int ) : Any

}
