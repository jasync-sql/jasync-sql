package com.github.mauricio.postgresql.messages.backend

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 12:42 AM
 */

object InformationMessage {

  val Severity = 'S'
  val SQLState = 'C'
  val Message = 'M'
  val Detail = 'D'
  val Hint = 'H'
  val Position = 'P'
  val InternalQuery = 'q'
  val Where = 'W'
  val File = 'F'
  val Line = 'L'
  val Routine = 'R'

  val Fields = Map(
    Severity -> "Severity",
    SQLState -> "SQLSTATE",
    Message -> "Message",
    Detail -> "Detail",
    Hint -> "Hint",
    Position -> "Position",
    InternalQuery -> "Internal Query",
    Where -> "Where",
    File -> "File",
    Line -> "Line",
    Routine -> "Routine"
  )

  def fieldName( name : Char ) : String = Fields.getOrElse(name, { name.toString } )

}

abstract class InformationMessage ( statusCode : Char, val fields : Map[Char,String] )
  extends Message( statusCode ) {

  def message : String = this.fields( 'M' )

  override def toString : String = {
    "%s(fields=%s)".format( this.getClass.getSimpleName, fields.map { pair => InformationMessage.fieldName(pair._1) -> pair._2 } )
  }

}