package com.github.mauricio.async.db.postgresql.exceptions

import com.github.mauricio.async.db.exceptions.DatabaseException

/**
 * User: mauricio
 * Date: 4/4/13
 * Time: 12:36 AM
 */
class DateEncoderNotAvailableException(value: Any)
  extends DatabaseException("There is no encoder for value [%s] of type %s".format(value, value.getClass.getCanonicalName))
