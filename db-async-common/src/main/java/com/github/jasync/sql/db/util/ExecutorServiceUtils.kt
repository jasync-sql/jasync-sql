
package com.github.jasync.sql.db.util

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object ExecutorServiceUtils {
  //TODO check usage in performance test - it looks it was used but this cache create a new thread each call
  //creating a lot of threads from that pool means we are not really reactive
  val CachedThreadPool = Executors.newCachedThreadPool(DaemonThreadsFactory("db-async-default"))

  fun newFixedPool( count : Int, name: String ) : ExecutorService {
    return Executors.newFixedThreadPool( count, DaemonThreadsFactory(name) )
  }

}
