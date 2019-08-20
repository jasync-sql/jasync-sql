package com.github.jasync.sql.db.mysql.memsql;

import com.github.jasync.sql.db.Configuration;
import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.mysql.MySQLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.util.concurrent.TimeUnit;

/**
 * See run-docker-mysql.sh to run a local instance of MySql.
 */
public class MemsqlContainerHelper {

    private static final Logger logger = LoggerFactory.getLogger(MemsqlContainerHelper.class);

    public Configuration defaultMemsqlConfiguration = new Configuration(
            "root",
            "localhost",
            3306,
            null,
            "memsql_async_tests"
    );

    public static MemSQLContainer mysql;

    public Integer getPort() {
        return defaultMemsqlConfiguration.getPort();
    }

    public void init() {
        try {
            new MySQLConnection(defaultMemsqlConfiguration).connect().get(1, TimeUnit.SECONDS);
            logger.info("Using local mysql instance {}", defaultMemsqlConfiguration);
        } catch (Exception e) {
            // If local instance isn't running, start a docker mysql on random port
            if (mysql == null) {
                mysql = new MemSQLContainer(defaultMemsqlConfiguration);
            }
            if (!mysql.isRunning()) {
                mysql.start();
            }
            defaultMemsqlConfiguration = new Configuration(mysql.getUsername(), "localhost", mysql.getFirstMappedPort(), mysql.getPassword(), mysql.getDatabaseName());
            //defaultMemsqlConfiguration = new Configuration("root", "localhost", mysql.getFirstMappedPort(), "test", "mysql_async_tests");
            logger.info("Using test container instance {}", defaultMemsqlConfiguration);
        } finally {
            try {
                Connection connection = new MySQLConnection(defaultMemsqlConfiguration).connect().get(1, TimeUnit.SECONDS);
//                connection.sendQuery("GRANT ALL PRIVILEGES ON *.* TO 'mysql_async'@'%' IDENTIFIED BY 'root' WITH GRANT OPTION;").get(1, TimeUnit.SECONDS);
//                QueryResult r = connection.sendQuery("select count(*) as cnt  from mysql.user where user = 'mysql_async_nopw';").get(1, TimeUnit.SECONDS);
//                if (r.getRows() != null && r.getRows().size() > 0) {
//                    RowData rd = r.getRows().get(0);
//                    Boolean exists = ((long) rd.get(0)) > 0;
//                    if (!exists) {
//                        connection.sendQuery("CREATE USER 'mysql_async_nopw'@'%'").get(1, TimeUnit.SECONDS);
//                    }
//                    connection.sendQuery("create table IF NOT EXISTS mysql_async_tests.transaction_test (id varchar(255) not null, primary key (id))").get(1, TimeUnit.SECONDS);
//                }
//                connection.sendQuery("GRANT ALL PRIVILEGES ON *.* TO 'mysql_async_nopw'@'%' WITH GRANT OPTION").get(1, TimeUnit.SECONDS);
                String createTable = "CREATE TABLE IF NOT EXISTS numbers (id BIGINT NOT NULL, number_double DOUBLE, PRIMARY KEY (id))";
                connection.sendQuery(createTable).get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        }
    }
}

class MemSQLContainer extends JdbcDatabaseContainer<MemSQLContainer> {

    private final Configuration configuration;

    public MemSQLContainer(Configuration configuration) {
        super(("memsql/quickstart"));
        this.configuration = configuration;
    }

    @Override
    public String getDriverClassName() {
        return "com.mysql.jdbc.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:mysql://" + getContainerIpAddress() + ":" + getMappedPort(configuration.getPort()) + "/" + configuration.getDatabase();
    }

    @Override
    public String getUsername() {
        return configuration.getUsername();
    }

    @Override
    public String getPassword() {
        return configuration.getPassword();
    }

    @Override
    protected String getTestQueryString() {
        return "select 1";
    }
}
