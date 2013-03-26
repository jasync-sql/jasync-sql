package com.github.mauricio.postgresql

import concurrent.Future


/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 10:40 PM
 */

trait Connection {

  def disconnect : Future[Connection]
  def isConnected : Boolean
  def sendQuery( query : String ) : Future[QueryResult]
  def sendPreparedStatement( query : String, values : Array[Any] = Array.empty[Any] ) : Future[QueryResult]

}
