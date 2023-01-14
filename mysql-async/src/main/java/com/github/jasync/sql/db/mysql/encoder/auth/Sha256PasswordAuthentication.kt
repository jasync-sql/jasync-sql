package com.github.jasync.sql.db.mysql.encoder.auth

import com.github.jasync.sql.db.util.length
import java.nio.charset.Charset

object Sha256PasswordAuthentication : AuthenticationMethod {

    private val EmptyArray = ByteArray(0)

    override fun generateAuthentication(charset: Charset, password: String?, seed: ByteArray?): ByteArray {
        return if (password != null) {
            val bytes = password.toByteArray(charset)
            val result = ByteArray(bytes.length + 1)
            System.arraycopy(bytes, 0, result, 0, bytes.length)
            result
        } else {
            EmptyArray
        }
    }
}
