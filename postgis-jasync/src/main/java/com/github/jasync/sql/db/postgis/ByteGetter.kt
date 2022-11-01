package com.github.jasync.sql.db.postgis

abstract class ByteGetter {
    /**
     * Get a byte.
     *
     * @param index the index to get the value from
     * @return The result is returned as Int to eliminate sign problems when
     * or'ing several values together.
     */
    abstract operator fun get(index: Int): Int
    class BinaryByteGetter(private val array: ByteArray) : ByteGetter() {
        override fun get(index: Int): Int {
            return array[index].toInt() and 0xFF // mask out sign-extended bits.
        }
    }

    class StringByteGetter(private val rep: String) : ByteGetter() {
        override fun get(index: Int): Int {
            var index = index
            index *= 2
            val high = unhex(rep[index]).toInt()
            val low = unhex(rep[index + 1]).toInt()
            return (high shl 4) + low
        }

        companion object {
            fun unhex(c: Char): Byte {
                return if (c >= '0' && c <= '9') {
                    (c.code - '0'.code).toByte()
                } else if (c >= 'A' && c <= 'F') {
                    (c.code - 'A'.code + 10).toByte()
                } else if (c >= 'a' && c <= 'f') {
                    (c.code - 'a'.code + 10).toByte()
                } else {
                    throw IllegalArgumentException("No valid Hex char $c")
                }
            }
        }
    }
}
