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

    "parse an array of numbers" in {

      val numbers = "{1,2,3}"
      val encoder = new ArrayEncoderDecoder( IntegerEncoderDecoder )

      val result = encoder.decode( numbers )

      result === List(1,2,3)
    }

    "parse an array of array of numbers" in {

      val numbers = "{{1,2,3},{4,5,6}}"
      val encoder = new ArrayEncoderDecoder( IntegerEncoderDecoder )

      val result = encoder.decode( numbers )

      result === List(List(1,2,3), List(4,5,6))
    }

  }

}
