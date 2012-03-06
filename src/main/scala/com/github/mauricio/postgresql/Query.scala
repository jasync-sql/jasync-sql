package com.github.mauricio.postgresql

import parsers.ColumnData
import org.jboss.netty.buffer.ChannelBuffer
import util.Log
import java.util.ArrayList

/**
 * User: Maurício Linhares
 * Date: 3/4/12
 * Time: 12:42 AM
 */

object Query {
  val log = Log.get[Query]
}

class Query ( val columnTypes : Array[ColumnData] ) {

  import CharsetHelper._

  private val rows = new ArrayList[Array[Any]]()
  private val columnMapping : Map[String, Int] = this.columnTypes.map {
    columnData =>
      (columnData.name, columnData.columnNumber - 1)
  }.toMap


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

    this.rows.add(realRow)

  }

  def getValue( columnNumber : Int, rowNumber : Int ) : Any = {
    this.rows.get( rowNumber )(columnNumber)
  }

  def getValue( columnName : String, rowNumber : Int ) : Any = {
    this.rows.get( rowNumber )( this.columnMapping( columnName ) )
  }

  def apply( name : String, row : Int ) : Any = {
    this.getValue( name, row)
  }

  def apply( column : Int,  row : Int ) : Any = {
    this.getValue(column, row)
  }

  def count : Int = {
    this.rows.size()
  }

}
