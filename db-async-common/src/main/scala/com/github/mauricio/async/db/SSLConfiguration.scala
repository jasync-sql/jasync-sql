package com.github.mauricio.async.db

import java.io.File

import SSLConfiguration.Mode

/**
 *
 * Contains the SSL configuration necessary to connect to a database.
 *
 * @param mode whether and with what priority a SSL connection will be negotiated, default disabled
 * @param rootCert path to PEM encoded trusted root certificates, None to use internal JDK cacerts, defaults to None
 *
 */
case class SSLConfiguration(mode: Mode.Value = Mode.Disable, rootCert: Option[java.io.File] = None)

object SSLConfiguration {

  object Mode extends Enumeration {
    val Disable    = Value("disable")      // only try a non-SSL connection
    val Prefer     = Value("prefer")       // first try an SSL connection; if that fails, try a non-SSL connection
    val Require    = Value("require")      // only try an SSL connection, but don't verify Certificate Authority
    val VerifyCA   = Value("verify-ca")    // only try an SSL connection, and verify that the server certificate is issued by a trusted certificate authority (CA)
    val VerifyFull = Value("verify-full")  // only try an SSL connection, verify that the server certificate is issued by a trusted CA and that the server host name matches that in the certificate
  }

  def apply(properties: Map[String, String]): SSLConfiguration = SSLConfiguration(
    mode = Mode.withName(properties.get("sslmode").getOrElse("disable")),
    rootCert = properties.get("sslrootcert").map(new File(_))
  )
}
