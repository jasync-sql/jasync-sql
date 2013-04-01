package com.github.mauricio.postgresql.util

import java.security._
import org.jboss.netty.util.CharsetUtil

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 6:19 PM
 *
 * Copied over and translated to Scala from org.postgresql.util.MD5Digest from the PostgreSQL JDBC driver.
 *
 */
object MD5Digest {

  val Lookup = Array[Byte]( '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

  def encode( username : String, password : String, salt : Array[Byte] ) : Array[Byte] = {

    val usernameBytes = username.getBytes(CharsetUtil.UTF_8)
    val passwordBytes = password.getBytes(CharsetUtil.UTF_8)
    val hexDigest = new Array[Byte](35)

    val messageDigest = MessageDigest.getInstance("MD5")

    messageDigest.update(usernameBytes)
    messageDigest.update(passwordBytes)

    val tempDigest = messageDigest.digest()

    bytesToHex(tempDigest, hexDigest, 0 )

    messageDigest.update(hexDigest, 0, 32)
    messageDigest.update(salt)

    val passwordDigest = messageDigest.digest()

    bytesToHex(passwordDigest, hexDigest, 3)
    hexDigest(0) = 'm'
    hexDigest(1) = 'd'
    hexDigest(2) = '5'

    hexDigest
  }

  private def bytesToHex( bytes : Array[Byte], hex : Array[Byte], offset : Int) {
    var pos = offset
    var i = 0

    while ( i < 16 ) {
      val c = bytes(i) & 0xFF
      var j = c >> 4
      pos += 1
      hex(pos) = Lookup(j)
      j = (c & 0xF)
      pos += 1
      hex(pos) = Lookup(j)
      i += 1
    }
  }

}