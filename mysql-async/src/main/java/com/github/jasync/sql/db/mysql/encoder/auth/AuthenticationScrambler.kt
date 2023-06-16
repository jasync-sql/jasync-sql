package com.github.jasync.sql.db.mysql.encoder.auth

import java.nio.charset.Charset
import java.security.MessageDigest
import kotlin.experimental.xor

object AuthenticationScrambler {

    fun scramble411(
        algorithm: String,
        password: String,
        charset: Charset,
        seed: ByteArray,
        seedFirst: Boolean
    ): ByteArray {
        val messageDigest = MessageDigest.getInstance(algorithm)
        val initialDigest = messageDigest.digest(password.toByteArray(charset))

        messageDigest.reset()

        val finalDigest = messageDigest.digest(initialDigest)

        messageDigest.reset()

        if (seedFirst) {
            messageDigest.update(seed)
            messageDigest.update(finalDigest)
        } else {
            messageDigest.update(finalDigest)
            messageDigest.update(seed)
        }

        val result = messageDigest.digest()
        for ((index, byte) in result.withIndex()) {
            result[index] = byte xor initialDigest[index]
        }

        return result
    }
}
