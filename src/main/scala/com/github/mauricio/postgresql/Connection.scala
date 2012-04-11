package com.github.mauricio.postgresql

import util.Future


/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 10:40 PM
 */

trait Connection {

  def disconnect
  def isConnected : Boolean
  def sendQuery( query : String ) : Future[Throwable,QueryResult]
  def sendPreparedStatement( query : String, values : Array[Any] = Array.empty[Any] ) : Future[Throwable, QueryResult]

}
