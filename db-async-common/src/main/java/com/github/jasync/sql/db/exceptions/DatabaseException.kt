
package com.github.jasync.sql.db.exceptions

open class DatabaseException(message: String, cause : Throwable?) : RuntimeException(message) {

  constructor( message : String ) : this(message, null)

}
