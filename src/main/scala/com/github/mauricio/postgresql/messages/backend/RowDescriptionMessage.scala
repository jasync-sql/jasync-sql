package com.github.mauricio.postgresql.messages.backend

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 1:15 AM
 */
case class RowDescriptionMessage ( val columnDatas : Array[ColumnData] )
  extends Message( Message.RowDescription )