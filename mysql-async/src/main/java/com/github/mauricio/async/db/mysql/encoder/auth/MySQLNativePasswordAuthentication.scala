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

package com.github.mauricio.async.db.mysql.encoder.auth

import java.nio.charset.Charset
import java.security.MessageDigest

object MySQLNativePasswordAuthentication extends AuthenticationMethod {

  final val EmptyArray = Array.empty[Byte]

  def generateAuthentication(charset : Charset, password: Option[String], seed : Array[Byte]): Array[Byte] = {

    if ( password.isDefined ) {
      scramble411(charset, password.get, seed )
    } else {
      EmptyArray
    }

  }

  private def scramble411(charset : Charset, password : String, seed : Array[Byte] ) : Array[Byte] = {

    val messageDigest = MessageDigest.getInstance("SHA-1")
    val initialDigest = messageDigest.digest(password.getBytes(charset))

    messageDigest.reset()

    val finalDigest = messageDigest.digest(initialDigest)

    messageDigest.reset()

    messageDigest.update(seed)
    messageDigest.update(finalDigest)

    val result = messageDigest.digest()
    var counter = 0

    while ( counter < result.length ) {
      result(counter) = (result(counter) ^ initialDigest(counter)).asInstanceOf[Byte]
      counter += 1
    }

    result
  }

}
