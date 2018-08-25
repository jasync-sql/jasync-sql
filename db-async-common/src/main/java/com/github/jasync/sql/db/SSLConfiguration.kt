package com.github.jasync.sql.db

import java.io.File

/**
 *
 * Contains the SSL configuration necessary to connect to a database.
 *
 * @param mode whether and , what priority a SSL connection will be negotiated, funault disabled
 * @param rootCert path to PEM encoded trusted root certificates, None to use internal JDK cacerts, funaults to None
 *
 */
data class SSLConfiguration(val mode: Mode = Mode.Disable, val rootCert: java.io.File? = null) {

  constructor(properties: Map<String, String>) :
      this(
          modeByValue(properties.getOrElse("sslmode") { "disable" }),
          properties.get("sslrootcert")?.let { File(it) }
      )

  enum class Mode(val valueName: String) {
    Disable("disable"),
    Prefer("prefer"),
    Require("require"),
    VerifyCA("verify-ca"),
    VerifyFull("verify-full");

  }


}

private fun modeByValue(value: String): SSLConfiguration.Mode = SSLConfiguration.Mode.values().first { it.valueName == value }

