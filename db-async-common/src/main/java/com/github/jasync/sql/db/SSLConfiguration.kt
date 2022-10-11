package com.github.jasync.sql.db

import java.io.File

/**
 *
 * Contains the SSL configuration necessary to connect to a database.
 *
 * @param mode whether and , what priority a SSL connection will be negotiated, default disabled
 * @param rootCert path to PEM encoded X.509 trusted root certificates, null to use internal JDK cacerts, defaults to null
 * @param clientCert path to PEM encoded X.509 client certificate chain, defaults to null
 * @param clientPrivateKey path to PKCS#8 private key file in PEM format, defaults to null
 *
 */
data class SSLConfiguration(
    val mode: Mode = Mode.Disable,
    val rootCert: File? = null,
    val clientCert: File? = null,
    val clientPrivateKey: File? = null
) {

    constructor(properties: Map<String, String>) :
        this(
            modeByValue(properties.getOrElse("sslmode") { "disable" }),
            properties["sslrootcert"]?.let { File(it) },
            properties["sslcert"]?.let { File(it) },
            properties["sslkey"]?.let { File(it) }
        )

    enum class Mode(val valueName: String) {
        Disable("disable"),
        Prefer("prefer"),
        Require("require"),
        VerifyCA("verify-ca"),
        VerifyFull("verify-full");
    }
}

private fun modeByValue(value: String): SSLConfiguration.Mode =
    SSLConfiguration.Mode.values().first { it.valueName == value }
