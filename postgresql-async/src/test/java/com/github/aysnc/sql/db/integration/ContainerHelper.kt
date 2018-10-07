package com.github.aysnc.sql.db.integration

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import mu.KotlinLogging
import org.testcontainers.containers.PostgreSQLContainer
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * See run-docker-postresql.sh to run a local instance of postgreSql.
 */
object ContainerHelper {

  var postresql: MyPostgreSQLContainer? = null

  val port: Int
    get() = defaultConfiguration.port

  /**
   * default config is a local instance already running on port 15432 (i.e. a docker postresql)
   */
  var defaultConfiguration = Configuration(
      "postresql_async",
      "localhost",
      15432,
      "root",
      "netty_driver_test")

  init {
    try {
      PostgreSQLConnection(defaultConfiguration).connect().get(1, TimeUnit.SECONDS)
      logger.info("Using local postresql instance $defaultConfiguration")
    } catch (e: Exception) {
      // If local instance isn't running, start a docker postresql on random port
      if (postresql == null) {
        postresql = MyPostgreSQLContainer()
            .withDatabaseName("netty_driver_test")
            .withPassword("root")
            .withUsername("postresql_async")//.withCopyFileToContainer();
      }
      if (!postresql!!.isRunning()) {
        postresql!!.start()
      }
      defaultConfiguration = Configuration(postresql!!.getUsername(), "localhost", postresql!!.getFirstMappedPort()!!, postresql!!.getPassword(), postresql!!.getDatabaseName())
      logger.info("PORT is " + defaultConfiguration.port)
      logger.info("Using test container instance {}", defaultConfiguration)
    } finally {
      try {
        val connection = PostgreSQLConnection(defaultConfiguration).connect().get(1, TimeUnit.SECONDS)
        logger.info("got connection " + connection.isConnected())
        //logger.info("select 1: " + connection.sendQuery("select 1").get().rowsAffected)
        connection.sendQuery("""
          DROP TYPE IF EXISTS example_mood; CREATE TYPE example_mood AS ENUM ('sad', 'ok', 'happy')
        """).get()
      } catch (e: Exception) {
        logger.error(e.localizedMessage, e)
      }

    }
  }

}
 class MyPostgreSQLContainer: PostgreSQLContainer<MyPostgreSQLContainer>("postgres:9.3")
