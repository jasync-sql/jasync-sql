package com.github.jasync.sql.db.mysql.encoder.auth

import java.nio.charset.Charset

object CachingSha2PasswordAuthentication : AuthenticationMethod {

    private val EmptyArray = ByteArray(0)

    override fun generateAuthentication(charset: Charset, password: String?, seed: ByteArray?): ByteArray {
        return if (password != null) {
            if (seed != null) {
                // Fast authentication mode. Requires seed, but not SSL.
                AuthenticationScrambler.scramble411("SHA-256", password, charset, seed, false)
            } else {
                // Full authentication mode.
                // Since this sends the plaintext password, SSL is required.
                // Without SSL, the server always rejects the password.
                Sha256PasswordAuthentication.generateAuthentication(charset, password, null)
            }
        } else {
            EmptyArray
        }
    }
}
