package com.github.mauricio.async.db.postgresql.util

import org.specs2.mutable.Specification
import io.netty.util.CharsetUtil

/**
 * User: mauricio
 * Date: 5/9/13
 * Time: 5:43 PM
 */
class PasswordHelperSpec extends Specification {

  val salt = Array[Byte](-31, 68, 99, 36)
  val result = Array[Byte](109,100,53,54,102,57,55,57,98,99,51,101,100,100,54,101,56,52,57,49,100,52,101,99,49,55,100,57,97,51,102,97,97,55,56)

  def printArray( name : String, bytes : Array[Byte] ) {
    printf("%s %s -> (%s)%n", name, bytes.length, bytes.mkString(","))
  }


  "helper" should {

    "generate the same value as the PostgreSQL code" in {

      val username = "mauricio"
      val password = "example"

      PasswordHelper.encode(username, password, salt, CharsetUtil.UTF_8) === result

    }

  }

}
