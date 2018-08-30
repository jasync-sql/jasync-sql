package com.github.jasync.sql.db.mysql;

import java.util.concurrent.TimeUnit;

import org.testcontainers.containers.MySQLContainer;

import com.github.jasync.sql.db.Configuration;

public class ContainerHelper {

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

  static {
    try {
      new MySQLConnection(defaultConfiguration).connect().get(1, TimeUnit.SECONDS);
    } catch (Exception e) {
      // If local instance isn't running, start a docker mysql on random port
      if (mysql == null){
        mysql = new MySQLContainer("mysql:5.7")
                  .withDatabaseName("mysql_async_tests")
                  .withPassword("root")
                  .withUsername("mysql_async");
      }
      if (!mysql.isRunning()) {
        mysql.start();
      }
      defaultConfiguration = new Configuration(mysql.getUsername(), "localhost", mysql.getFirstMappedPort(), mysql.getPassword(), mysql.getDatabaseName());
    }
  }
}