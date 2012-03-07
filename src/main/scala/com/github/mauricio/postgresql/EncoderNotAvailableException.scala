package com.github.mauricio.postgresql

/**
 * User: Maurício Linhares
 * Date: 3/4/12
 * Time: 12:19 AM
 */

class EncoderNotAvailableException( message : FrontendMessage )
  extends IllegalArgumentException( "Encoder not available for name %s".format( message.kind ) )
