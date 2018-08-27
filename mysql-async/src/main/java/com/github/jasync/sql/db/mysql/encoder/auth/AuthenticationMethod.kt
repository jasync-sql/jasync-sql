
package com.github.jasync.sql.db.mysql.encoder.auth

import java.nio.charset.Charset



interface AuthenticationMethod {

  fun generateAuthentication( charset : Charset, password : String?, seed : ByteArray ) : ByteArray

  companion object {
    val Native = "mysql_native_password"
    val Old = "mysql_old_password"

    val Availables = mapOf(
        Native to MySQLNativePasswordAuthentication,
        Old to OldPasswordAuthentication
    )
  }
}
