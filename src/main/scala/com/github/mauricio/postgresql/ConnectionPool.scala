package com.github.mauricio.postgresql

import org.apache.commons.pool.impl.StackObjectPool

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 10:38 PM
 */

class ConnectionPool(
                          val host : String,
                          val port : Int,
                          val user: String,
                          val database: String) {

  private val factory = new ConnectionObjectFactory(host, port, user, database)
  private val pool = new StackObjectPool( this.factory, 1 )

  def doWithConnection[T]( fn : Connection => T ) : T = {
    val borrowed = this.pool.borrowObject()
    try {
      fn(borrowed)
    } finally {
      this.pool.returnObject(borrowed)
    }
  }

}
