package com.github.mauricio.postgresql.exceptions

/**
 * User: mauricio
 * Date: 4/4/13
 * Time: 12:36 AM
 */
class DateEncoderNotAvailableException( value : Any )
  extends IllegalArgumentException( "There is no encoder for value [%s] of type %s".format(value, value.getClass.getCanonicalName) )
