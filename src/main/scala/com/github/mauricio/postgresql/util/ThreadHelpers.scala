package com.github.mauricio.postgresql.util

/**
 * User: Maurício Linhares
 * Date: 3/10/12
 * Time: 10:06 AM
 */

object ThreadHelpers {

  def safeSleep( milis : Long ) {

    val current = System.currentTimeMillis()
    var slept = false
    while ( !slept ) {
      Thread.sleep(milis)
      if ( (current + milis) <= System.currentTimeMillis() ) {
        slept = true
      }
    }

  }

}
