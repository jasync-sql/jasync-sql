package com.github.jasync.sql.db.mysql;

import com.github.jasync.sql.db.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.MySQLContainer;

import java.security.cert.CertificateException;
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

    private static void startMySQLDocker() throws CertificateException {
        if (mysql == null) {
            // MySQLContainer always sets the root password to be the same as the
            // user password. For legacy reasons, we expect the root password to be
            // different.
            mysql = new MySQLContainer("mysql:5.7.32") {
                @Override
                protected void configure() {
                    super.configure();
                    // Make sure to do this after the call to `super` so these
                    // really do override the environment variables.
                    addEnv("MYSQL_DATABASE", "mysql_async_tests");
                    addEnv("MYSQL_USER", "mysql_async");
                    addEnv("MYSQL_PASSWORD", "root");
                    addEnv("MYSQL_ROOT_PASSWORD", "test");
                }
            };
            mysql.withClasspathResourceMapping("server.key.txt", "/docker-entrypoint-initdb.d/server-key.pem", BindMode.READ_ONLY);
            mysql.withClasspathResourceMapping("server.cert.txt", "/docker-entrypoint-initdb.d/server-cert.pem", BindMode.READ_ONLY);
            mysql.withClasspathResourceMapping("update-config.sh", "/docker-entrypoint-initdb.d/update-config.sh", BindMode.READ_ONLY);
        }
        if (!mysql.isRunning()) {
            mysql.withStartupTimeoutSeconds(60).start();
        }
        defaultConfiguration = new Configuration("mysql_async", "localhost", mysql.getFirstMappedPort(), "root", "mysql_async_tests");
        rootConfiguration = new Configuration("root", "localhost", mysql.getFirstMappedPort(), "test", "mysql_async_tests");
        logger.info("Using test container instance {}", defaultConfiguration);
    }

    private static void configureDatabase() throws Exception {
        Connection connection = new MySQLConnection(rootConfiguration).connect().get(1, TimeUnit.SECONDS);
        connection.sendQuery("GRANT ALL PRIVILEGES ON *.* TO 'mysql_async'@'%' IDENTIFIED BY 'root' WITH GRANT OPTION;").get(1, TimeUnit.SECONDS);
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
