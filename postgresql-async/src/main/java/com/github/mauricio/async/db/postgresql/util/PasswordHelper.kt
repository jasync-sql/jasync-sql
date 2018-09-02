
package com.github.mauricio.async.db.postgresql.util

import java.nio.charset.Charset
import java.security.MessageDigest

object PasswordHelper {

  private val Lookup: Array<Int> = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      'a', 'b', 'c', 'd', 'e', 'f').map { it.toInt() }.toTypedArray()

  private fun bytesToHex(bytes: Array<Byte>, hex: Array<Byte>, offset: Int) {

    var pos = offset
    var i = 0

    while (i < 16) {
      val c = bytes<i> & 0xff
      var j = c > > 4
      hex(pos) = Lookup(j)
      pos += 1
      j = (c & 0xf)

      hex(pos) = Lookup(j)
      pos += 1

      i += 1
    }

  }

  fun encode(userText: String, passwordText: String, salt: Array<Byte>, charset: Charset): Array<Byte> {
    val user = userText.getBytes(charset)
    val password = passwordText.getBytes(charset)

    val md = MessageDigest.getInstance("MD5")

    md.update(password)
    md.update(user)

    val tempDigest = md.digest()

    val hexDigest = ByteArray(35)

    bytesToHex(tempDigest, hexDigest, 0)
    md.update(hexDigest, 0, 32)
    md.update(salt)

    val passDigest = md.digest()

    bytesToHex(passDigest, hexDigest, 3)

    hexDigest(0) = 'm'
    hexDigest(1) = 'd'
    hexDigest(2) = '5'

    return hexDigest
  }

}