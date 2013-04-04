package com.github.mauricio.postgresql

import java.util.concurrent.ExecutorService
import java.nio.charset.Charset
import org.jboss.netty.util.CharsetUtil

/**
 * User: mauricio
 * Date: 3/29/13
 * Time: 12:05 AM
 */

object Configuration {
  val Default = new Configuration("postgres")
}


case class Configuration ( val username : String,
                           val host : String = "localhost",
                           val port : Int = 5432,
                           val password : Option[String] = None,
                           val database : Option[String] = None,
                           val bossPool : ExecutorService = ExecutorServiceUtils.CachedThreadPool,
                           val workerPool : ExecutorService = ExecutorServiceUtils.CachedThreadPool,
                           val charset : Charset = CharsetUtil.UTF_8
                           )
