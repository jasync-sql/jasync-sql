package com.github.mauricio.postgresql.messages.backend

import com.github.mauricio.postgresql.column.ColumnEncoderDecoder

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

  val decoder = ColumnEncoderDecoder.decoderFor( this.dataType )

}
