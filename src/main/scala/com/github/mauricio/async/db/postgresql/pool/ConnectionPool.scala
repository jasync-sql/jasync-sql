/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.postgresql.pool

import com.github.mauricio.async.db.postgresql.pool.ConnectionObjectFactory
import com.github.mauricio.async.db.{Configuration, Connection}
import org.apache.commons.pool.impl.StackObjectPool

class ConnectionPool(val configuration: Configuration) {

  private val factory = new ConnectionObjectFactory(configuration)
  private val pool = new StackObjectPool(this.factory, 1)

  def doWithConnection[T](fn: Connection => T): T = {
    val borrowed = this.pool.borrowObject()
    try {
      fn(borrowed)
    } finally {
      this.pool.returnObject(borrowed)
    }
  }

}
