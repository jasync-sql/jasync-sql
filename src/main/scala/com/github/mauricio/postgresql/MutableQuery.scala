package com.github.mauricio.postgresql

import messages.backend.ColumnData
import org.jboss.netty.buffer.ChannelBuffer
import util.Log
import collection.mutable.ArrayBuffer

/**
 * User: Maurício Linhares
 * Date: 3/4/12
 * Time: 12:42 AM
 */

object MutableQuery {
  val log = Log.get[MutableQuery]
}

class MutableQuery ( val columnTypes : Array[ColumnData] ) extends ResultSet {

  import CharsetHelper._

  private val rows = new ArrayBuffer[Array[Any]]()
  private val columnMapping : Map[String, Int] = this.columnTypes.map {
    columnData =>
      (columnData.name, columnData.columnNumber - 1)
  }.toMap

  override def length: Int = this.rows.length

  override def apply(idx: Int): Array[Any] = this.rows(idx)

  def update(idx: Int, elem: Array[Any]) = this.rows(idx) = elem

  def addRawRow( row : Array[ChannelBuffer] ) {

    val realRow = new Array[Any](row.length)

    0.until(row.length).foreach {
      index =>

        realRow(index) = if ( row(index) == null ) {
          null
        } else {
          this.columnTypes(index).decoder.decode( row(index).toString(Unicode) )
        }

    }

    this.rows += realRow
  }

  def getValue( columnNumber : Int, rowNumber : Int ) : Any = {
    this.rows( rowNumber )(columnNumber)
  }

  def getValue( columnName : String, rowNumber : Int ) : Any = {
    this.rows( rowNumber )( this.columnMapping( columnName ) )
  }

  def apply( name : String, row : Int ) : Any = this.getValue( name, row)

  def apply( column : Int,  row : Int ) : Any = this.getValue(column, row)

}
