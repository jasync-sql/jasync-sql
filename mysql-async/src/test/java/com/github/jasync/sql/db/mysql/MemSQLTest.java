package com.github.jasync.sql.db.mysql;

import com.github.jasync.sql.db.Configuration;
import com.github.jasync.sql.db.QueryResult;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * See run-docker-memsql.sh to run a local instance of MySql.
 */
public class MemSQLTest {
	public static Configuration defaultConfiguration = new Configuration(
            "root",
            "localhost",
            3306,
            null,
            "memsql_async_tests");

	public String createTable = "CREATE TABLE IF NOT EXISTS numbers (id BIGINT NOT NULL, number_double DOUBLE, PRIMARY KEY (id))";

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
		assertTrue(res.getRowsAffected() == 1);
		res = conn.sendQuery("truncate table numbers").get();
		conn.disconnect().get(1, TimeUnit.SECONDS);
	}

	@Test
	public void testPreparedStatement() throws InterruptedException, ExecutionException, TimeoutException {
		MySQLConnection conn = setup();
		QueryResult res = conn.sendPreparedStatement("insert into numbers (id, number_double) VALUES (?, ?);",
			Arrays.asList(19, 13)).get();
		assertTrue(res.getRowsAffected() == 1);
		res = conn.sendQuery("truncate table numbers").get();
		conn.disconnect().get(1, TimeUnit.SECONDS);
	}

	private MySQLConnection setup() throws InterruptedException, ExecutionException, TimeoutException {
		MySQLConnection connection = new MySQLConnection(defaultConfiguration).connect().get(1, TimeUnit.SECONDS);
		connection.sendQuery(createTable).get(10, TimeUnit.SECONDS);
		return connection;
	}

}

