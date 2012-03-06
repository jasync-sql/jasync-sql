package com.github.mauricio.postgresql

/**
 * User: Maurício Linhares
 * Date: 3/4/12
 * Time: 12:19 AM
 */

class EncoderNotAvailableException( message : Message )
  extends IllegalArgumentException( "Encoder not available for name %s - %s".format( message.name, message.content ) )
