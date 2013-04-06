package com.github.mauricio.async.db.util

import org.slf4j.LoggerFactory


/**
 * User: Maurício Linhares
 * Date: 2/25/12
 * Time: 8:01 PM
 */

object Log {

  def get[T](implicit tag : reflect.ClassTag[T]) = {
    LoggerFactory.getLogger( tag.runtimeClass.getName )
  }

  def getByName( name : String ) = {
    LoggerFactory.getLogger(name)
  }

}
