
package com.github.mauricio.postgresql

import java.util.concurrent.atomic.AtomicInteger

object TestUtils {

  private val count = AtomicInteger()

  fun nextInt: Int {
    count.incrementAndGet()
  }

}