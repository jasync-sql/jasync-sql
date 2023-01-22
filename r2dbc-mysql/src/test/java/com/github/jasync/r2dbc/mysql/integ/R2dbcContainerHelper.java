package com.github.jasync.r2dbc.mysql.integ;

import com.github.jasync.sql.db.Configuration;
import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.QueryResult;
import com.github.jasync.sql.db.RowData;
import com.github.jasync.sql.db.SSLConfiguration;
import com.github.jasync.sql.db.SSLConfiguration.Mode;
import com.github.jasync.sql.db.mysql.MySQLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.MySQLContainer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * See run-docker-mysql.sh to run a local instance of MySql.
 */
public class R2dbcContainerHelper {
    private static final Logger logger = LoggerFactory.getLogger(R2dbcContainerHelper.class);

    public static MySQLContainer<?> mysql;

    public static Integer getPort() {
        return defaultConfiguration.getPort();
    }

    private static final SSLConfiguration sslConfiguration =
        new SSLConfiguration(Mode.Prefer, null, null, null);

    /**
     * default config is a local instance already running on port 33306 (i.e. a docker mysql)
     */
    public static Configuration defaultConfiguration = new Configuration(
        "mysql_async",
        "localhost",
        33306,
        "root",
        "mysql_async_tests",
        sslConfiguration);

    /**
     * config for container.
     */
    private static Configuration rootConfiguration = new Configuration(
        "root",
        "localhost",
        33306,
        "test",
        "mysql_async_tests",
        sslConfiguration);

    public static List<String> configurationFiles = Arrays.asList(
        "ca.pem",
        "server-key.pem",
        "server-cert.pem",
        "private_key.pem",
        "public_key.pem",
        "update-config.sh");

    private static boolean isLocalMySQLRunning() {
        try {
            new MySQLConnection(rootConfiguration)
                .connect().get(1, TimeUnit.SECONDS)
                .disconnect().get(1, TimeUnit.SECONDS);
            logger.info("Using local mysql instance {}", defaultConfiguration);
            return true;
        } catch (Exception e) {

            return false;
        }
    }

    private static void startMySQLDocker() {
        if (mysql == null) {
            mysql = new MySQLContainer("mysql:8.0.31") {
                @Override
                protected void configure() {
                    super.configure();
                    // Make sure to do this after the call to `super` so these
                    // really do override the environment variables.
                    // MySQLContainer always sets the root password to be the same as the
                    // user password. For legacy reasons, we expect the root password to be
                    // different.
                    addEnv("MYSQL_ROOT_PASSWORD", "test");
                }
            }.withUsername("mysql_async")
             .withPassword("root")
             .withDatabaseName("mysql_async_tests");

            for (String file : configurationFiles) {
                mysql.withClasspathResourceMapping(file, "/docker-entrypoint-initdb.d/" + file, BindMode.READ_ONLY);
            }
        }
        if (!mysql.isRunning()) {
            mysql.start();
        }
        defaultConfiguration = new Configuration("mysql_async", "localhost", mysql.getFirstMappedPort(), "root", "mysql_async_tests", sslConfiguration);
        rootConfiguration = new Configuration("root", "localhost", mysql.getFirstMappedPort(), "test", "mysql_async_tests", sslConfiguration);
        logger.info("Using test container instance {}", defaultConfiguration);
    }

    private static void configureDatabase() throws Exception {
        Connection connection = new MySQLConnection(rootConfiguration).connect().get(1, TimeUnit.SECONDS);
        connection.sendQuery("GRANT ALL PRIVILEGES ON *.* TO 'mysql_async'@'%' WITH GRANT OPTION;").get(1, TimeUnit.SECONDS);
        QueryResult r = connection.sendQuery("select count(*) as cnt  from mysql.user where user = 'mysql_async_nopw';").get(1, TimeUnit.SECONDS);
        r.getRows();
        if (r.getRows().size() > 0) {
            RowData rd = r.getRows().get(0);
            boolean exists = rd.getLong(0) > 0;
            if (!exists) {
                connection.sendQuery("CREATE USER 'mysql_async_nopw'@'%'").get(1, TimeUnit.SECONDS);
            }
            connection.sendQuery("create table IF NOT EXISTS mysql_async_tests.transaction_test (id varchar(255) not null, primary key (id))").get(1, TimeUnit.SECONDS);
        }
        connection.sendQuery("GRANT ALL PRIVILEGES ON *.* TO 'mysql_async_nopw'@'%' WITH GRANT OPTION").get(1, TimeUnit.SECONDS);
        connection.disconnect().get(1, TimeUnit.SECONDS);
    }

    static {
        try {
            if (!isLocalMySQLRunning()) {
                // If local instance isn't running, start a docker mysql on random port
                startMySQLDocker();
            }
            configureDatabase();
        } catch (Exception e) {
            String containerLogs = mysql != null ? "\nContainer logs: " + mysql.getLogs() : "";
            logger.error(e.getLocalizedMessage() + containerLogs, e);
            throw new IllegalStateException(e);
        }
    }
}
