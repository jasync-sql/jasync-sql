package com.github.mauricio.postgresql.util

import org.apache.log4j.{Level, Logger}


/**
 * User: Maurício Linhares
 * Date: 2/25/12
 * Time: 8:01 PM
 */

object Log {
  val FQCN = "com.github.mauricio.postgresql.util.Log"

  def get[T](implicit manifest : Manifest[T] ) : Log = {
    new Log( Logger.getLogger( manifest.erasure ) )
  }

}

class Log ( private val log : Logger ) {

  def debug( message : String,  values : Any* ) : Unit = {
    this.log.debug( Log.FQCN, Level.DEBUG, message.format( values : _* ) )
  }

  def error( message : String,  values : Any* ) : Unit = {
    this.log.debug( Log.FQCN, Level.ERROR, message.format( values : _* ) )
  }

}
