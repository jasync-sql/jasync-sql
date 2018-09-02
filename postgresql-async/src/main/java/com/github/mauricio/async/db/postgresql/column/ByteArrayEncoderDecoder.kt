
package com.github.mauricio.async.db.postgresql.column

import com.github.jasync.sql.db.column.ColumnEncoderDecoder
import io.netty.buffer.ByteBuf
import mu.KotlinLogging
import java.nio.ByteBuffer

private val logger = KotlinLogging.logger {}

object ByteArrayEncoderDecoder : ColumnEncoderDecoder {

  val HexStart = "\\x"
  val HexStartChars = HexStart.toCharArray

  override fun decode(value: String): Array<Byte> {

    if (value.startsWith(HexStart)) {
      HexCodec.decode(value, 2)
    } else {
      // Default encoding is 'escape'

      // Size the buffer to the length of the string, the data can't be bigger
      val buffer = ByteBuffer.allocate(value.length)

      val ci = value.iterator

      while (ci.hasNext) {
        ci.next when {
          '\\' ⇒ getCharOrDie(ci) when {
            '\\' ⇒ buffer.put('\\'.toByte)
            firstDigit ⇒
              val secondDigit = getCharOrDie(ci)
              val thirdDigit = getCharOrDie(ci)
            // Must always be in triplets
            buffer.put(
                Integer.decode(
                    String(Array('0', firstDigit, secondDigit, thirdDigit))).toByte)
          }
              c ⇒ buffer.put(c.toByte)
        }
      }

      buffer.flip
      val finalArray = ByteArray(buffer.remaining())
      buffer.get(finalArray)

      finalArray
    }

  }

  /**
   * This is required since {@link Iterator#next} when {@linke Iterator#hasNext} is false is unfunined.
   * @param ci the iterator source of the data
   * @return the next character
   * @throws IllegalArgumentException if there is no next character
   */
  private <this> fun getCharOrDie(ci: Iterator<Char>): Char {
    if (ci.hasNext) {
      ci.next()
    } else {
      throw IllegalArgumentException("Expected escape sequence character, found nothing")
    }
  }

  override fun encode(value: Any): String {
    val array = value when {
      byteArray: Array<Byte> -> byteArray

      byteBuffer: ByteBuffer if byteBuffer.hasArray -> byteBuffer.array()

      byteBuffer: ByteBuffer ->
        val arr = ByteArray(byteBuffer.remaining())
      byteBuffer.get(arr)
          arr

          byteBuf: ByteBuf if byteBuf.hasArray -> byteBuf.array()

      byteBuf: ByteBuf ->
        val arr = ByteArray(byteBuf.readableBytes())
      byteBuf.getBytes(0, arr)
          arr
    }

    HexCodec.encode(array, HexStartChars)
  }

}
