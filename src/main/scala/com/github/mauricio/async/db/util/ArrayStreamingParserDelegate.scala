package com.github.mauricio.async.db.util

/**
 * User: mauricio
 * Date: 4/19/13
 * Time: 1:32 PM
 */

trait ArrayStreamingParserDelegate {

  def arrayStarted : Unit = {}
  def arrayEnded : Unit = {}
  def elementFound( element : String ) : Unit = {}
  def nullElementFound : Unit = {}

}
