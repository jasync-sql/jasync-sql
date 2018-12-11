package com.github.jasync.sql.db.mysql;

import com.github.jasync.sql.db.Configuration;
import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.QueryResult;
import com.github.jasync.sql.db.RowData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;

import java.util.concurrent.TimeUnit;

/**
 * See run-docker-mysql.sh to run a local instance of MySql.
 */
public class ContainerHelper {
    private static final Logger logger = LoggerFactory.getLogger(ContainerHelper.class);

    public static MySQLContainer mysql;

    public static Integer getPort() {
        return defaultConfiguration.getPort();
    }

    /**
     * default config is a local instance already running on port 33306 (i.e. a docker mysql)
     */
    public static Configuration defaultConfiguration = new Configuration(
            "mysql_async",
            "localhost",
            33306,
            "root",
            "mysql_async_tests");

    /**
     * config for container.
     */
    private static Configuration rootConfiguration = new Configuration(
            "root",
            "localhost",
            33306,
            "test",
            "mysql_async_tests");


    static {
        try {
            new MySQLConnection(rootConfiguration).connect().get(1, TimeUnit.SECONDS);
            logger.info("Using local mysql instance {}", defaultConfiguration);
        } catch (Exception e) {
            // If local instance isn't running, start a docker mysql on random port
            if (mysql == null) {
                mysql = new MySQLContainer("mysql:5.7")
                        .withDatabaseName("mysql_async_tests")
                        .withPassword("root")
                        .withUsername("mysql_async");
            }
            if (!mysql.isRunning()) {
                mysql.start();
            }
            defaultConfiguration = new Configuration(mysql.getUsername(), "localhost", mysql.getFirstMappedPort(), mysql.getPassword(), mysql.getDatabaseName());
            rootConfiguration = new Configuration("root", "localhost", mysql.getFirstMappedPort(), "test", "mysql_async_tests");
            logger.info("Using test container instance {}", defaultConfiguration);
        } finally {
            try {
                Connection connection = new MySQLConnection(rootConfiguration).connect().get(1, TimeUnit.SECONDS);
                connection.sendQuery("GRANT ALL PRIVILEGES ON *.* TO 'mysql_async'@'%' IDENTIFIED BY 'root' WITH GRANT OPTION;").get(1, TimeUnit.SECONDS);
                QueryResult r = connection.sendQuery("select count(*) as cnt  from mysql.user where user = 'mysql_async_nopw';").get(1, TimeUnit.SECONDS);
                if (r.getRows() != null && r.getRows().size() > 0) {
                    RowData rd = r.getRows().get(0);
                    Boolean exists = ((long) rd.get(0)) > 0;
                    if (!exists) {
                        connection.sendQuery("CREATE USER 'mysql_async_nopw'@'%'").get(1, TimeUnit.SECONDS);
                    }
                    connection.sendQuery("create table IF NOT EXISTS mysql_async_tests.transaction_test (id varchar(255) not null, primary key (id))").get(1, TimeUnit.SECONDS);
                }
                connection.sendQuery("GRANT ALL PRIVILEGES ON *.* TO 'mysql_async_nopw'@'%' WITH GRANT OPTION").get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        }
    }
}
