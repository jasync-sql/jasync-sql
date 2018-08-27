
package com.github.jasync.sql.db.util

//reusing KotlinVersion as it has the same semantics
typealias Version = KotlinVersion

fun parseVersion(version: String): Version {
  val splitted = version.split(".")
  return Version(splitted[0].toIntOrNull() ?: 0,
      splitted[1].toIntOrNull() ?: 0,
      splitted[2].toIntOrNull() ?: 0
  )
}
