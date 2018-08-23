package com.github.mauricio.async.db.mysql.encoder.auth

import java.nio.charset.Charset
import java.nio.CharBuffer

object OldPasswordAuthentication extends AuthenticationMethod {

  final val EmptyArray = Array.empty[Byte]

  def generateAuthentication(charset: Charset, password: Option[String], seed: Array[Byte]): Array[Byte] = {
    password match {
      case Some(value) if !value.isEmpty => {
        newCrypt(charset, value, new String(seed, charset) )
      }
      case _ => EmptyArray
    }
  }

  def newCrypt( charset: Charset, password : String, seed : String) : Array[Byte] = {
    var b : Byte = 0
    var d : Double = 0

    val pw = newHash(seed)
    val msg = newHash(password)
    val max = 0x3fffffffL
    var seed1 = (pw._1 ^ msg._1) % max
    var seed2 = (pw._2 ^ msg._2) % max
    val chars = new Array[Char](seed.length)

    var i = 0
    while ( i < seed.length ) {
      seed1 = ((seed1 * 3) + seed2) % max
      seed2 = (seed1 + seed2 + 33) % max
      d =  seed1.toDouble / max.toDouble
      b =  java.lang.Math.floor((d * 31) + 64).toByte
      chars(i) = b.toChar
      i += 1
    }

    seed1 = ((seed1 * 3) + seed2) % max
    seed2 = (seed1 + seed2 + 33) % max
    d = seed1.toDouble / max.toDouble
    b = java.lang.Math.floor(d * 31).toByte

    var j = 0
    while( j < seed.length ) {
      chars(j) = (chars(j) ^ b).toChar
      j += 1
    }

    val bytes = new String(chars).getBytes(charset)
    val result = new Array[Byte](bytes.length + 1)
    System.arraycopy(bytes, 0, result, 0, bytes.length )
    result
  }

  private def newHash(password : String) : (Long,Long) = {
    var nr = 1345345333L
    var add = 7L
    var nr2 = 0x12345671L
    var tmp = 0L

    password.foreach {
      c =>
      if (c != ' ' && c != '\t') {
        tmp = (0xff & c)
        nr ^= ((((nr & 63) + add) * tmp) + (nr << 8))
        nr2 += ((nr2 << 8) ^ nr)
        add += tmp
      }
    }

    (nr & 0x7fffffffL, nr2 & 0x7fffffffL)
  }

}
