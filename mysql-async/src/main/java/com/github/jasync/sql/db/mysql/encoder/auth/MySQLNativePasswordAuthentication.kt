package com.github.jasync.sql.db.mysql.encoder.auth

import com.github.jasync.sql.db.SSLConfiguration
import java.nio.charset.Charset
import java.nio.file.Path

object MySQLNativePasswordAuthentication : AuthenticationMethod {

    private val EmptyArray = ByteArray(0)

    override fun generateAuthentication(
        charset: Charset,
        password: String?,
        seed: ByteArray,
        sslConfiguration: SSLConfiguration,
        rsaPublicKey: Path?
    ): ByteArray {
        return if (password != null) {
            AuthenticationScrambler.scramble411("SHA-1", password, charset, seed, true)
        } else {
            EmptyArray
        }
    }
}
