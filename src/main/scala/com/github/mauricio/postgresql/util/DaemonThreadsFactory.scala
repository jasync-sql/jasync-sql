package com.github.mauricio.postgresql.util

import java.util.concurrent.{Executors, ThreadFactory}


/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 2:22 PM
 */

object DaemonThreadsFactory extends ThreadFactory {
  def newThread(r: Runnable): Thread = {

    val thread = Executors.defaultThreadFactory().newThread(r)
    thread.setDaemon(true)

    return thread
  }
}
