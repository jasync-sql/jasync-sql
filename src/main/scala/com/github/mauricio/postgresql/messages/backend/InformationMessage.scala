package com.github.mauricio.postgresql.messages.backend

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 12:42 AM
 */

object InformationMessage {

  val Fields = Map(
    'S' -> "Severity",
    'C' -> "SQLSTATE",
    'M' -> "Message",
    'D' -> "Detail",
    'H' -> "Hint",
    'P' -> "Position",
    'q' -> "Internal Query",
    'W' -> "Where",
    'F' -> "File",
    'L' -> "Line",
    'R' -> "Routine"
  )

  def fieldName( name : Char ) : String = Fields.getOrElse(name, { name.toString } )

}

abstract case class InformationMessage ( statusCode : Char, val fields : Map[String,String] )
  extends Message( statusCode ) {

  def message : String = this.fields( "Message" )

}