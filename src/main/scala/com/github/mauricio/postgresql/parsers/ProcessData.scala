package com.github.mauricio.postgresql.parsers

/**
 * User: Maurício Linhares
 * Date: 2/28/12
 * Time: 11:13 PM
 */

class ProcessData ( val processId : Int, val secretKey : Int ) {

  override def toString : String = {
    "ProcessData: %s - %s".format( this.processId, secretKey)
  }

}
