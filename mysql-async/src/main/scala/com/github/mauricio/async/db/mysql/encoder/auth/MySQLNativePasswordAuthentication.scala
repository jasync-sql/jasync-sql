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
import org.jboss.netty.util.CharsetUtil

object MySQLNativePasswordAuthentication {
  final val EmptyArray = Array.empty[Byte]
}

class MySQLNativePasswordAuthentication( charset : Charset ) extends AuthenticationMethod {

  import MySQLNativePasswordAuthentication.EmptyArray

  def generateAuthentication(username: String, password: Option[String], seed : String): Array[Byte] = {

    if ( password.isDefined ) {
      scramble411(password.get, seed)
    } else {
      EmptyArray
    }

  }

  private def scramble411( password : String, seed : String ) : Array[Byte] = {

    val messageDigest = MessageDigest.getInstance("SHA-1")
    val initialDigest = messageDigest.digest(password.getBytes(charset))

    messageDigest.reset()

    val finalDigest = messageDigest.digest(initialDigest)

    messageDigest.reset()

    messageDigest.update(seed.getBytes(CharsetUtil.US_ASCII))
    messageDigest.update(finalDigest)

    val result = messageDigest.digest()
    var counter = 0

    while ( counter < result.length ) {
      result(counter) = (result(counter) ^ finalDigest(counter)).asInstanceOf[Byte]
      counter += 1
    }

    result
  }

}
