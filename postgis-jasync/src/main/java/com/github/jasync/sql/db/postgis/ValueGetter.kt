package com.github.jasync.sql.db.postgis

abstract class ValueGetter(data: ByteGetter, endian: Byte) {
    var data: ByteGetter
    var position = 0
    val endian: Byte

    init {
        this.data = data
        this.endian = endian
    }

    /**
     * Get a byte, should be equal for all endians
     *
     * @return the byte value
     */
    val byte: Byte
        get() = data.get(position++).toByte()
    val int: Int
        get() {
            val res = getInt(position)
            position += 4
            return res
        }
    val long: Long
        get() {
            val res = getLong(position)
            position += 8
            return res
        }

    /**
     * Get a 32-Bit integer
     *
     * @param index the index to get the value from
     * @return the int value
     */
    protected abstract fun getInt(index: Int): Int

    /**
     * Get a long value. This is not needed directly, but as a nice side-effect
     * from GetDouble.
     *
     * @param index the index to get the value from
     * @return the long value
     */
    protected abstract fun getLong(index: Int): Long

    /**
     * Get a double.
     *
     * @return the double value
     */
    val double: Double
        get() {
            val bitrep = long
            return java.lang.Double.longBitsToDouble(bitrep)
        }

    class XDR(data: ByteGetter) : ValueGetter(data, NUMBER) {
        override fun getInt(index: Int): Int {
            return (
                (data.get(index) shl 24) + (data.get(index + 1) shl 16) +
                    (data.get(index + 2) shl 8) + data.get(index + 3)
                )
        }

        override fun getLong(index: Int): Long {
            return (
                (data.get(index).toLong() shl 56) + (data.get(index + 1).toLong() shl 48) +
                    (data.get(index + 2).toLong() shl 40) + (data.get(index + 3).toLong() shl 32) +
                    (data.get(index + 4).toLong() shl 24) + (data.get(index + 5).toLong() shl 16) +
                    (data.get(index + 6).toLong() shl 8) + (data.get(index + 7).toLong() shl 0)
                )
        }

        companion object {
            const val NUMBER: Byte = 0
        }
    }

    class NDR(data: ByteGetter) : ValueGetter(data, NUMBER) {
        override fun getInt(index: Int): Int {
            return (
                (data.get(index + 3) shl 24) + (data.get(index + 2) shl 16) +
                    (data.get(index + 1) shl 8) + data.get(index)
                )
        }

        override fun getLong(index: Int): Long {
            return (
                (data.get(index + 7).toLong() shl 56) + (data.get(index + 6).toLong() shl 48) +
                    (data.get(index + 5).toLong() shl 40) + (data.get(index + 4).toLong() shl 32) +
                    (data.get(index + 3).toLong() shl 24) + (data.get(index + 2).toLong() shl 16) +
                    (data.get(index + 1).toLong() shl 8) + (data.get(index).toLong() shl 0)
                )
        }

        companion object {
            const val NUMBER: Byte = 1
        }
    }
}
