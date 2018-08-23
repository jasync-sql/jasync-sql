
package com.github.mauricio.async.db.exceptions

class DateEncoderNotAvailableException(value: Any)
  : DatabaseException("There is no encoder for value <%s> of type %s".format(value, value::class.java.canonicalName))
