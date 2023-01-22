package com.github.jasync.sql.db.mysql.encoder.auth

import com.github.jasync.sql.db.Configuration
import java.nio.charset.Charset

interface AuthenticationMethod {

    fun generateAuthentication(charset: Charset, configuration: Configuration, seed: ByteArray): ByteArray

    companion object {
        const val CachingSha2 = "caching_sha2_password"
        const val Native = "mysql_native_password"
        const val Old = "mysql_old_password"
        const val Sha256 = "sha256_password"

        val Availables = mapOf(
            CachingSha2 to CachingSha2PasswordAuthentication,
            Native to MySQLNativePasswordAuthentication,
            Old to OldPasswordAuthentication,
            Sha256 to Sha256PasswordAuthentication,
        )
    }
}
