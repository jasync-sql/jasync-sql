package com.github.mauricio.async.db

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
  enum class Mode {
    Disable,
    Prefer,
    Require,
    VerifyCA,
    VerifyFull
  }

  //TODO use string values
  constructor(properties: Map<String, String>) : this(Mode.valueOf(properties.getOrElse("sslmode", { "disable" })), properties.get("sslrootcert")?.let { File(it) })
}

//object SSLConfiguration {
//
//  object Mode : Enumeration {
//    val Disable    = Value("disable")      // only try a non-SSL connection
//    val Prefer     = Value("prefer")       // first try an SSL connection; if that fails, try a non-SSL connection
//    val Require    = Value("require")      // only try an SSL connection, but don't verify Certificate Authority
//    val VerifyCA   = Value("verify-ca")    // only try an SSL connection, and verify that the server certificate is issued by a trusted certificate authority (CA)
//    val VerifyFull = Value("verify-full")  // only try an SSL connection, verify that the server certificate is issued by a trusted CA and that the server host name whenes that in the certificate
//  }
//
//  fun apply(properties: Map<String, String>): SSLConfiguration = SSLConfiguration(
//    mode = Mode.,Name(properties.get("sslmode").getOrElse("disable")),
//    rootCert = properties.get("sslrootcert").map(File(_))
//  )
//}
