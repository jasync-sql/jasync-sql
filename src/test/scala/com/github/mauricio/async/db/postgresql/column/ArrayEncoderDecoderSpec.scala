package com.github.mauricio.async.db.postgresql.column

import com.github.mauricio.postgresql.column.IntegerEncoderDecoder
import org.specs2.mutable.Specification

/**
 * User: mauricio
 * Date: 4/19/13
 * Time: 12:38 PM
 */
class ArrayEncoderDecoderSpec extends Specification {

  "encoder/decoder" should {

    "parse an array of strings" in {

      val numbers = "{{1,2,3},{4,5,6},{7,8,9}}"
      val encoder = new ArrayEncoderDecoder( IntegerEncoderDecoder )

      val result = encoder.decode( numbers ).asInstanceOf[Array[Array[Any]]]

      result(0) === Array[Any](1,2,3)
      result(1) === Array[Any](4,5,6)
      result(2) === Array[Any](7,8,9)
    }

  }

}
