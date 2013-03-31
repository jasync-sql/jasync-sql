package com.github.mauricio.postgresql.messages.frontend

import com.github.mauricio.postgresql.messages.backend.AuthenticationResponseType

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 1:43 AM
 */
class CredentialMessage(
                         val username : String,
                         val password : String,
                         val kind : AuthenticationResponseType.AuthenticationResponseType )