package com.github.mauricio.postgresql

import java.util.concurrent.atomic.AtomicInteger

/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 9:49 PM
 */

object TestUtils {

  private val count = new AtomicInteger()

  def nextInt : Int = {
    count.incrementAndGet()
  }

}
