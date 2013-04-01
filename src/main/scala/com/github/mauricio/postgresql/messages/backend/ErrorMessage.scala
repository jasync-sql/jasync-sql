package com.github.mauricio.postgresql.messages.backend

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 12:57 AM
 */
class ErrorMessage ( fields : Map[Char,String] )
  extends InformationMessage( Message.Error, fields )
