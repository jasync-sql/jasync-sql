package com.github.mauricio.postgresql.exceptions

/**
 * User: mauricio
 * Date: 3/30/13
 * Time: 11:23 PM
 */
class UnsupportedAuthenticationMethodException ( val authenticationType : Int )
  extends IllegalArgumentException ( "Unknown authentication method -> '%s'".format(authenticationType) )