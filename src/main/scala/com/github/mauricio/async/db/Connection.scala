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

package com.github.mauricio.async.db

import concurrent.Future

trait Connection {

  def disconnect : Future[Connection]
  def connect : Future[Map[String,String]]
  def isConnected : Boolean
  def sendQuery( query : String ) : Future[QueryResult]
  def sendPreparedStatement( query : String, values : Array[Any] = Array.empty[Any] ) : Future[QueryResult]

}
