package com.github.mauricio.postgresql.pool

import org.apache.commons.pool.PoolableObjectFactory
import concurrent.duration._
import concurrent.Await
import com.github.mauricio.postgresql.{Configuration, DatabaseConnectionHandler}

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 10:40 PM
 */

class ConnectionObjectFactory(
                              configuration : Configuration)
  extends PoolableObjectFactory[DatabaseConnectionHandler] {

  def makeObject(): DatabaseConnectionHandler = {
    val connection = new DatabaseConnectionHandler(configuration)
    Await.result( connection.connect, 5 seconds )
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
