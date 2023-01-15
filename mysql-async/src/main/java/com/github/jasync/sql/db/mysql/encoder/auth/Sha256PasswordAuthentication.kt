package com.github.jasync.sql.db.mysql.encoder.auth

import com.github.jasync.sql.db.util.length
import java.nio.charset.Charset

// TODO: Implement public key encryption.
object Sha256PasswordAuthentication : AuthenticationMethod {

    private val EmptyArray = ByteArray(0)

    override fun generateAuthentication(charset: Charset, password: String?, seed: ByteArray?): ByteArray {
        return if (password != null) {
            val bytes = password.toByteArray(charset)
            val result = ByteArray(bytes.length + 1)
            bytes.copyInto(result)
            result
        } else {
            EmptyArray
        }
    }
}
