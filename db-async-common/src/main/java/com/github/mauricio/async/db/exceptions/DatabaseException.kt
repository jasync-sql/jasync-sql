
package com.github.mauricio.async.db.exceptions

open class DatabaseException(message: String, cause : Throwable?) : RuntimeException(message) {

  constructor( message : String ) : this(message, null)

}
