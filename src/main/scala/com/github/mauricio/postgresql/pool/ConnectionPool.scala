package com.github.mauricio.postgresql.pool

import org.apache.commons.pool.impl.StackObjectPool
import com.github.mauricio.postgresql.{Configuration, Connection}

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 10:38 PM
 */

class ConnectionPool( val configuration : Configuration ) {

  private val factory = new ConnectionObjectFactory(configuration)
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
