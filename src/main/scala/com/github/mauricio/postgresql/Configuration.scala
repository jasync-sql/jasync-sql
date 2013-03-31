package com.github.mauricio.postgresql

import java.util.concurrent.ExecutorService

/**
 * User: mauricio
 * Date: 3/29/13
 * Time: 12:05 AM
 */

object Configuration {
  val Default = new Configuration("postgres")
}


case class Configuration ( username : String,
                           host : String = "localhost",
                           port : Int = 5432,
                           password : Option[String] = None,
                           database : Option[String] = None,
                           bossPool : ExecutorService = ExecutorServiceUtils.CachedThreadPool,
                           workerPool : ExecutorService = ExecutorServiceUtils.CachedThreadPool
                           )
