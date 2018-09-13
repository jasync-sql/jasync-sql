
package com.github.mauricio.async.db.postgresql.column

import com.github.mauricio.async.db.column.IntegerEncoderDecoder
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import org.specs2.mutable.Specification

class ArrayDecoderSpec : Specification {

  fun execute( data : String ) : Any {
    val numbers = data.getBytes( CharsetUtil.UTF_8 )
    val encoder = ArrayDecoder(IntegerEncoderDecoder)
    encoder.decode(null, Unpooled.wrappedBuffer(numbers), CharsetUtil.UTF_8)
  }

  "encoder/decoder" should {

    "parse an array of numbers" in {
      execute("{1,2,3}") === List(1, 2, 3)
    }

    "parse an array of array of numbers" in {
      execute("{{1,2,3},{4,5,6}}") === List(List(1, 2, 3), List(4, 5, 6))
    }

  }

}