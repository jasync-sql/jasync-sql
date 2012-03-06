package com.github.mauricio.postgresql

import org.apache.commons.pool.PoolableObjectFactory
import java.util.concurrent.TimeUnit

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 10:40 PM
 */

class ConnectionObjectFactory(
                               val host : String,
                               val port : Int,
                               val user: String,
                               val database: String) extends PoolableObjectFactory[DatabaseConnectionHandler] {

  def makeObject(): DatabaseConnectionHandler = {
    val connection = new DatabaseConnectionHandler(host, port, user, database)
    connection.connect.get(5, TimeUnit.SECONDS)
    connection
  }

  def destroyObject(obj: DatabaseConnectionHandler) {
    obj.disconnect
  }

  def validateObject(obj: DatabaseConnectionHandler): Boolean = {
    obj.isConnected
  }

  def activateObject(obj: DatabaseConnectionHandler) {
    //no op
  }

  def passivateObject(obj: DatabaseConnectionHandler) {
    //no op
  }
}
