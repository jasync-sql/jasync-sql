package com.github.mauricio.postgresql.exceptions

import com.github.mauricio.postgresql.messages.backend.ErrorMessage

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 1:00 AM
 */
class DatabaseException( val errorMessage : ErrorMessage )
  extends IllegalStateException( errorMessage.message )