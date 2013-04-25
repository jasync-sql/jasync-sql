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

package com.github.mauricio.async.db.postgresql.pool

import scala.util.Try

/**
 *
 * Definition for objects that can be used as a factory for [[com.github.mauricio.async.db.postgresql.pool.AsyncObjectPool]]
 * objects.
 *
 * @tparam T
 */

trait ObjectFactory[T] {

  /**
   *
   * Creates a valid object to be used in the pool. This method can block if necessary to make sure a correctly built
   * is created.
   *
   * @return
   */

  def create : T

  /**
   *
   * This method should "close" and release all resources acquired by the pooled object. This object will not be used
   * anymore so any cleanup necessary to remove it from memory should be made in this method. Implementors should not
   * raise an exception under any circumstances, the factory should log and clean up the exception itself.
   *
   * @param item
   */

  def destroy( item : T )

  /**
   *
   * Validates that an object can still be used for it's purpose. This method should test the object to make sure
   * it's still valid for clients to use. If you have a database connection, test if you are still connected, if you're
   * accessing a file system, make sure you can still see and change the file.
   *
   * If this object is not valid anymore, a [[scala.util.Failure]] should be returned, otherwise [[scala.util.Success]]
   * should be the result of this call.
   *
   * @param item
   * @return
   */

  def validate( item : T ) : Try[T]

}