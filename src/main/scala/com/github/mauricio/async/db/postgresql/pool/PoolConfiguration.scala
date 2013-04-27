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

object PoolConfiguration {
  val Default = new PoolConfiguration(10, 4, 10)
}

/**
 *
 * Defines specific pieces of a pool's behavior.
 *
 * @param maxObjects how many objects this pool will hold
 * @param maxIdle how long are objects going to be kept as idle (not in use by clients of the pool)
 * @param maxQueueSize when there are no more objects, the pool can queue up requests to serve later then there
 *                     are objects available, this is the maximum number of enqueued requests
 * @param validationInterval pools will use this value as the timer period to validate idle objects.
 */

case class PoolConfiguration(
                              maxObjects: Int,
                              maxIdle: Long,
                              maxQueueSize: Int,
                              validationInterval: Long = 5000
                              )
