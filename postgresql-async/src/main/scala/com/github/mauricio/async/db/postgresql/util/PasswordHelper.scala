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

package com.github.mauricio.async.db.postgresql.util

import java.nio.charset.Charset
import java.security.MessageDigest

object PasswordHelper {

  private final val Lookup = Array[Byte]('0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'a', 'b', 'c', 'd', 'e', 'f')

  private def bytesToHex( bytes : Array[Byte], hex : Array[Byte], offset : Int ) {

    var pos = offset
    var i = 0

    while ( i < 16 ) {
      val c = bytes(i) & 0xff
      var j = c >> 4
      hex(pos) = Lookup(j)
      pos += 1
      j = (c & 0xf)

      hex(pos) = Lookup(j)
      pos += 1

      i += 1
    }

  }

  def encode( userText : String, passwordText : String, salt : Array[Byte], charset : Charset ) : Array[Byte] = {
    val user = userText.getBytes(charset)
    val password = passwordText.getBytes(charset)

    val md = MessageDigest.getInstance("MD5")

    md.update(password)
    md.update(user)

    val tempDigest = md.digest()

    val hexDigest = new Array[Byte](35)

    bytesToHex(tempDigest, hexDigest, 0)
    md.update(hexDigest, 0, 32)
    md.update(salt)

    val passDigest = md.digest()

    bytesToHex(passDigest, hexDigest, 3)

    hexDigest(0) = 'm'
    hexDigest(1) = 'd'
    hexDigest(2) = '5'

    hexDigest
  }

}
