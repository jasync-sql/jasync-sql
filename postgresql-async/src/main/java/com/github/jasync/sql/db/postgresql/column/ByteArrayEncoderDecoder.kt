package com.github.jasync.sql.db.postgresql.column

import com.github.jasync.sql.db.column.ColumnEncoderDecoder
import com.github.jasync.sql.db.util.HexCodec
import com.github.jasync.sql.db.util.XXX
import io.netty.buffer.ByteBuf
import java.nio.ByteBuffer

object ByteArrayEncoderDecoder : ColumnEncoderDecoder {

    private const val HexStart = "\\x"
    private val HexStartChars = HexStart.toCharArray()

    override fun decode(value: String): ByteArray {

        return if (value.startsWith(HexStart)) {
            HexCodec.decode(value, 2)
        } else {
            // Default encoding is 'escape'

            // Size the buffer to the length of the string, the data can't be bigger
            val buffer = ByteBuffer.allocate(value.length)

            val ci = value.iterator()

            while (ci.hasNext()) {
                val nextCi1 = ci.next()
                when (nextCi1) {
                    '\\' -> {
                        val nextCi2 = getCharOrDie(ci)
                        when (nextCi2) {
                            '\\' -> buffer.put('\\'.toByte())
                            else -> {
                                val secondDigit = getCharOrDie(ci)
                                val thirdDigit = getCharOrDie(ci)
                                // Must always be in triplets
                                buffer.put(
                                    Integer.decode(
                                        "0$nextCi2$secondDigit$thirdDigit"
                                    ).toByte()
                                )
                            }
                        }
                    }
                    else -> buffer.put(nextCi1.toByte())
                }
            }

            buffer.flip()
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
    private fun getCharOrDie(ci: Iterator<Char>): Char {
        return if (ci.hasNext()) {
            ci.next()
        } else {
            throw IllegalArgumentException("Expected escape sequence character, found nothing")
        }
    }

    override fun encode(value: Any): String {
        val array: ByteArray = when {
            value is ByteArray -> value

            value is ByteBuffer && value.hasArray() -> value.array()

            value is ByteBuffer -> {
                val arr = ByteArray(value.remaining())
                value.get(arr)
                arr
            }
            value is ByteBuf && value.hasArray() -> {
                value.array()
            }

            value is ByteBuf -> {
                val arr = ByteArray(value.readableBytes())
                value.getBytes(0, arr)
                arr
            }
            else -> XXX("$value - ${value.javaClass}")
        }

        return HexCodec.encode(array, HexStartChars)
    }
}
