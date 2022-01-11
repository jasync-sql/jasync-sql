package com.github.jasync.sql.db.mysql;

import com.github.jasync.sql.db.Configuration;
import com.github.jasync.sql.db.QueryResult;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * See run-docker-memsql.sh to run a local instance of MemSql before starting the test.
 *
 * The tests are ignored due to move to github actions (couldn't run memsql with 2 cores)
 */
@Ignore
public class MemSQLTest {
	private static Configuration defaultConfiguration = new Configuration(
            "root",
            "localhost",
            4306,
            null,
            "memsql_async_tests");

	private static String createTable = "CREATE TABLE IF NOT EXISTS numbers (id BIGINT NOT NULL, number_double DOUBLE, PRIMARY KEY (id))";

	@Test
	 public void testConnect() throws InterruptedException, ExecutionException, TimeoutException {
	 	MySQLConnection conn = setup();
	 	assertTrue(conn.isConnected());
	 	setup().disconnect().get(1, TimeUnit.SECONDS);
	 }

	@Test
	public void testSimpleQuery() throws InterruptedException, ExecutionException, TimeoutException {
		MySQLConnection conn = setup();
		QueryResult res = conn.sendQuery("select count(*) from numbers;").get();
		assertEquals(res.getRows().get(0).get(0), 0L);
		conn.disconnect().get(1, TimeUnit.SECONDS);
	}

	@Test
	public void testSimpleInsert() throws InterruptedException, ExecutionException, TimeoutException {
		MySQLConnection conn = setup();
		QueryResult res = conn.sendQuery("insert into numbers (id, number_double) VALUES (1, 2)").get();
		assertEquals(1, res.getRowsAffected());
		conn.sendQuery("truncate table numbers").get();
		conn.disconnect().get(1, TimeUnit.SECONDS);
	}

	@Test
	public void testPreparedStatement() throws InterruptedException, ExecutionException, TimeoutException {
		MySQLConnection conn = setup();
		QueryResult res = conn.sendPreparedStatement("insert into numbers (id, number_double) VALUES (?, ?);",
			Arrays.asList(19, 13)).get();
		assertEquals(1, res.getRowsAffected());
		conn.sendQuery("truncate table numbers").get();
		conn.disconnect().get(1, TimeUnit.SECONDS);
	}

	private MySQLConnection setup() throws InterruptedException, ExecutionException, TimeoutException {
		new PrepareMemsql().execute();
		return new MySQLConnection(defaultConfiguration).connect().get(10, TimeUnit.SECONDS);
	}

	private static class PrepareMemsql {
		static final String DB_URL = "jdbc:mysql://localhost:4306/?allowMultiQueries=true";

		static final String USER = "root";
		static final String PASS = null;

		void execute() {
			Connection conn;
			Statement stmt;
			try{
				Class.forName("com.mysql.jdbc.Driver");

				System.out.println("Connecting to database...");
				conn = DriverManager.getConnection(DB_URL,USER,PASS);

				System.out.println("Creating statement...");
				stmt = conn.createStatement();
				String sql;
				sql = "CREATE DATABASE IF NOT EXISTS memsql_async_tests; USE memsql_async_tests; " + MemSQLTest.createTable;
				stmt.execute(sql);
				stmt.close();
				conn.close();
			}catch(Exception e){
				throw new RuntimeException(e);
			}
		}
	}
}



