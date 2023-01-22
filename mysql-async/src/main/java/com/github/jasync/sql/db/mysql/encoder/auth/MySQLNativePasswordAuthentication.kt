package com.github.jasync.sql.db.mysql.encoder.auth

import com.github.jasync.sql.db.Configuration
import java.nio.charset.Charset

object MySQLNativePasswordAuthentication : AuthenticationMethod {

    private val EmptyArray = ByteArray(0)

    override fun generateAuthentication(charset: Charset, configuration: Configuration, seed: ByteArray): ByteArray {
        val password = configuration.password

        return if (password != null) {
            AuthenticationScrambler.scramble411("SHA-1", password, charset, seed, true)
        } else {
            EmptyArray
        }
    }
}
