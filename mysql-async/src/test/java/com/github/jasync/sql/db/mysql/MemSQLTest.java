package com.github.jasync.sql.db.mysql;

import com.github.jasync.sql.db.Configuration;
import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.QueryResult;
import com.github.jasync.sql.db.RowData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.junit.Test;
import static org.junit.Assert.*;
import com.github.jasync.sql.db.mysql.MySQLConnection;
import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;
import java.util.concurrent.TimeoutException;
import java.util.Arrays;
import com.github.jasync.sql.db.ResultSet;

import java.util.concurrent.TimeUnit;

/**
 * See run-docker-mysql.sh to run a local instance of MySql.
 */
public class MemSQLTest {
	public static Configuration defaultConfiguration = new Configuration(
            "root",
            "localhost",
            3306,
            null,
            "memsql_async_tests");

	public String createTable = "CREATE TABLE numbers (id BIGINT NOT NULL, number_double DOUBLE, PRIMARY KEY (id))";

	//@Test
	// public void testConnect() throws InterruptedException, ExecutionException, TimeoutException {
	// 	System.out.println("hello.");
	// 	MySQLConnection conn = setup();
	// 	assertTrue(conn.isConnected());
	// 	setup().disconnect().get(1, TimeUnit.SECONDS);
	// }

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
		return new MySQLConnection(defaultConfiguration).connect().get(1, TimeUnit.SECONDS);
	}

}

