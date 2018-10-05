package com.github.aysnc.sql.db.integration;

import com.github.jasync.sql.db.Configuration;
import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.concurrent.TimeUnit;

/**
 * See run-docker-postresql.sh to run a local instance of postreSql.
 */
public class ContainerHelper {
    protected static final Logger log = LoggerFactory.getLogger(ContainerHelper.class);

    public static PostgreSQLContainer postresql;

    public static Integer getPort() {
        return defaultConfiguration.getPort();
    }

    /**
     * default config is a local instance already running on port 33306 (i.e. a docker postresql)
     */
    public static Configuration defaultConfiguration = new Configuration(
            "postresql_async",
            "localhost",
            33306,
            "root",
            "postresql_async_tests");

    /**
     * config for container.
     */
    private static Configuration rootConfiguration = new Configuration(
            "root",
            "localhost",
            33306,
            "test",
            "postresql_async_tests");


    static {
        try {
            new PostgreSQLConnection(rootConfiguration).connect().get(1, TimeUnit.SECONDS);
            log.info("Using local postresql instance {}", defaultConfiguration);
        } catch (Exception e) {
            // If local instance isn't running, start a docker postresql on random port
            if (postresql == null) {
                postresql = new PostgreSQLContainer("postgresql:9.3")
                        .withDatabaseName("postresql_async_tests")
                        .withPassword("root")
                        .withUsername("postresql_async");
            }
            if (!postresql.isRunning()) {
                postresql.start();
            }
            defaultConfiguration = new Configuration(postresql.getUsername(), "localhost", postresql.getFirstMappedPort(), postresql.getPassword(), postresql.getDatabaseName());
            rootConfiguration = new Configuration("root", "localhost", postresql.getFirstMappedPort(), "test", "postresql_async_tests");
            log.info("Using test container instance {}", defaultConfiguration);
        } finally {
            try {
                Connection connection = new PostgreSQLConnection(rootConfiguration).connect().get(1, TimeUnit.SECONDS);
//        connection.sendQuery("GRANT ALL PRIVILEGES ON *.* TO 'mysql_async'@'%' IDENTIFIED BY 'root' WITH GRANT OPTION;").get(1, TimeUnit.SECONDS);
//        QueryResult r  = connection.sendQuery("select count(*) as cnt  from mysql.user where user = 'mysql_async_nopw';").get(1, TimeUnit.SECONDS);
//        if (r.getRows() != null && r.getRows().size() > 0) {
//          RowData rd = r.getRows().get(0);
//          Boolean exists = ((Long) rd.get(0)) > 0;
//          if (!exists) {
//            connection.sendQuery("CREATE USER 'mysql_async_nopw'@'%'").get(1, TimeUnit.SECONDS);
//          }
//          connection.sendQuery("create table IF NOT EXISTS mysql_async_tests.transaction_test (id varchar(255) not null, primary key (id))").get(1, TimeUnit.SECONDS);
//        }
//        connection.sendQuery("GRANT ALL PRIVILEGES ON *.* TO 'mysql_async_nopw'@'%' WITH GRANT OPTION").get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }
    }
}
