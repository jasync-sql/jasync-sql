package com.github.jasync.sql.db.util

import mu.KotlinLogging
import java.util.concurrent.ExecutorService

private val logger = KotlinLogging.logger {}

class Worker(private val executionContext: ExecutorService) {

  companion object {

    operator fun invoke(): Worker = Worker(ExecutorServiceUtils.newFixedPool(1, "db-sql-worker"))

  }

  fun action(f: () -> Unit) {
    this.executionContext.execute {
      try {
        f()
      } catch (e: Exception) {
        logger.error("Failed to execute task %s".format(f), e)
      }
    }
  }

  fun shutdown() {
    this.executionContext.shutdown()
  }

}
