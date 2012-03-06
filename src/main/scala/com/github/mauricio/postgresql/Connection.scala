package com.github.mauricio.postgresql

import java.util.concurrent.Future

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 10:40 PM
 */

trait Connection {

  def disconnect
  def isConnected : Boolean
  def sendQuery( query : String )(fn : QueryResult => Unit) : Future[QueryResult]

}
