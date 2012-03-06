package com.github.mauricio.postgresql.util

import org.slf4j.{Logger, LoggerFactory}


/**
 * User: Maurício Linhares
 * Date: 2/25/12
 * Time: 8:01 PM
 */

object Log {

  def get[T](implicit manifest : Manifest[T] ) = {
    LoggerFactory.getLogger( manifest.erasure.getName )
  }

  def getByName( name : String ) = {
    LoggerFactory.getLogger(name)
  }

}
