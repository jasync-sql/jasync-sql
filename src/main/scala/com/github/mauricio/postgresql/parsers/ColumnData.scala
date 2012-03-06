package com.github.mauricio.postgresql.parsers

import com.github.mauricio.postgresql.column.ColumnDecoder

/**
 * User: Maurício Linhares
 * Date: 3/1/12
 * Time: 10:33 PM
 */

class ColumnData(
  val name: String,
  val tableObjectId: Int,
  val columnNumber: Int,
  val dataType: Int,
  val dataTypeSize: Int,
  val dataTypeModifier: Int,
  val fieldFormat: Int ) {

  val decoder = ColumnDecoder.decoderFor( this.dataType )

}
