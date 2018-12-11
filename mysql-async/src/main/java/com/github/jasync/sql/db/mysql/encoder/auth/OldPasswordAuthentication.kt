package com.github.jasync.sql.db.mysql.encoder.auth

import com.github.jasync.sql.db.util.length
import java.nio.charset.Charset

object OldPasswordAuthentication : AuthenticationMethod {

    val EmptyArray = ByteArray(0)

    override fun generateAuthentication(charset: Charset, password: String?, seed: ByteArray): ByteArray {
        return when {
            password != null && password.isNotEmpty() -> {
                newCrypt(charset, password, String(seed, charset))
            }
            else -> EmptyArray
        }
    }

    fun newCrypt(charset: Charset, password: String, seed: String): ByteArray {
        var b: Byte = 0
        var d: Double = 0.0

        val pw = newHash(seed)
        val msg = newHash(password)
        val max = 0x3fffffffL
        var seed1 = (pw.first xor msg.first) % max
        var seed2 = (pw.second xor msg.second) % max
        val chars = CharArray(seed.length)

        var i = 0
        while (i < seed.length) {
            seed1 = ((seed1 * 3) + seed2) % max
            seed2 = (seed1 + seed2 + 33) % max
            d = seed1.toDouble() / max.toDouble()
            b = java.lang.Math.floor((d * 31) + 64).toByte()
            chars[i] = b.toChar()
            i += 1
        }

        seed1 = ((seed1 * 3) + seed2) % max
        seed2 = (seed1 + seed2 + 33) % max
        d = seed1.toDouble() / max.toDouble()
        b = java.lang.Math.floor(d * 31).toByte()

        var j = 0
        while (j < seed.length) {
            chars[j] = (chars[j].toInt() xor b.toInt()).toChar()
            j += 1
        }

        val bytes = String(chars).toByteArray(charset)
        val result = ByteArray(bytes.length + 1)
        System.arraycopy(bytes, 0, result, 0, bytes.length)
        return result
    }

    private fun newHash(password: String): Pair<Long, Long> {
        var nr = 1345345333L
        var add = 7L
        var nr2 = 0x12345671L
        var tmp = 0L

        password.forEach { c ->
            if (c != ' ' && c != '\t') {
                tmp = (0xff and c.toInt()).toLong()
                nr = nr xor (((((nr and 63) + add) * tmp) + (nr shl 8)))
                nr2 += ((nr2 shl 8) xor nr)
                add += tmp
            }
        }

        return (nr and 0x7fffffffL) to (nr2 and 0x7fffffffL)
    }

}
