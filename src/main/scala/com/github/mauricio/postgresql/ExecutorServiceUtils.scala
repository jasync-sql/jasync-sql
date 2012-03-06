package com.github.mauricio.postgresql

import java.util.concurrent.Executors
import util.DaemonThreadsFactory

/**
 * User: Maurício Linhares
 * Date: 3/5/12
 * Time: 9:25 PM
 */

object ExecutorServiceUtils {
  val CachedThreadPool = Executors.newCachedThreadPool( DaemonThreadsFactory )
}
