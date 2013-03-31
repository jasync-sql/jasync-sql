package com.github.mauricio.postgresql.messages.backend

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 12:22 AM
 */
object AuthenticationResponseType extends Enumeration {
  type AuthenticationResponseType = Value
  val MD5, Cleartext, Ok = Value
}
