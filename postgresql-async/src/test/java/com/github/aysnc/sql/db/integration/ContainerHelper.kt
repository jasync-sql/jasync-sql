package com.github.aysnc.sql.db.integration

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import java.util.concurrent.TimeUnit
import mu.KotlinLogging
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.PostgreSQLContainer

private val logger = KotlinLogging.logger {}

private val version = "9.3"

/**
 * See run-docker-postgresql.sh to run a local instance of postgreSql.
 */
object ContainerHelper {
    var postgresql: MyPostgreSQLContainer? = null

    /**
     * default config is a local instance already running on port 15432 (i.e. a docker postgresql)
     */
    var defaultConfiguration = Configuration(
        "postresql_async",
        "localhost",
        15432,
        "root",
        "netty_driver_test"
    )

    init {
        try {
            PostgreSQLConnection(defaultConfiguration).connect().get(1, TimeUnit.SECONDS)
            logger.info("Using local postgresql instance $defaultConfiguration")
        } catch (e: Exception) {
            // If local instance isn't running, start a docker postgresql on random port
            if (postgresql == null) {
                configurePostgres()
            }
            if (!postgresql!!.isRunning()) {
                postgresql!!.start()
            }
            defaultConfiguration = Configuration(
                postgresql!!.getUsername(),
                "localhost",
                postgresql!!.getFirstMappedPort(),
                postgresql!!.getPassword(),
                postgresql!!.getDatabaseName()
            )
            logger.info("PORT is " + defaultConfiguration.port)
            logger.info("Using test container instance {}", defaultConfiguration)
        } finally {
            try {
                val connection = PostgreSQLConnection(defaultConfiguration).connect().get(1, TimeUnit.SECONDS)
                logger.info("got connection " + connection.isConnected())
                connection.sendQuery(
                """
          DROP TYPE IF EXISTS example_mood; CREATE TYPE example_mood AS ENUM ('sad', 'ok', 'happy')
        """
                ).get()
                connection.sendQuery(
                """
          CREATE USER postgres_cleartext WITH PASSWORD 'postgres_cleartext'; GRANT ALL PRIVILEGES ON DATABASE ${defaultConfiguration.database} to postgres_cleartext;
          CREATE USER postgres_md5 WITH PASSWORD 'postgres_md5'; GRANT ALL PRIVILEGES ON DATABASE ${defaultConfiguration.database} to postgres_md5;
          CREATE USER postgres_kerberos WITH PASSWORD 'postgres_kerberos'; GRANT ALL PRIVILEGES ON DATABASE ${defaultConfiguration.database} to postgres_kerberos;
        """
                ).get()
            } catch (e: Exception) {
                logger.error(e.localizedMessage, e)
            }
        }
    }

    private fun configurePostgres() {
        postgresql = MyPostgreSQLContainer()
            .withDatabaseName("netty_driver_test")
            .withPassword("root")
            .withUsername("postresql_async")
            .withClasspathResourceMapping("pg_hba.conf", "/docker-entrypoint-initdb.d/pg_hba.conf", BindMode.READ_WRITE)
            .withClasspathResourceMapping("server.cert.txt", "/docker-entrypoint-initdb.d/server.crt", BindMode.READ_WRITE)
            .withClasspathResourceMapping("server.key.txt", "/docker-entrypoint-initdb.d/server.key", BindMode.READ_WRITE)
            .withClasspathResourceMapping("update-config.sh", "/docker-entrypoint-initdb.d/update-config.sh", BindMode.READ_WRITE)
    }
}

class MyPostgreSQLContainer : PostgreSQLContainer<MyPostgreSQLContainer>("postgres:$version")
