/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.async.db.mysql

import com.github.mauricio.async.db.util.FutureUtils.awaitFuture
import com.github.mauricio.async.db._
import com.github.mauricio.async.db.pool.{PoolConfiguration, ConnectionPool}
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory
import scala.Some

trait ConnectionHelper {

  val createTableNumericColumns =
    """
      |create temporary table numbers (
      |id int auto_increment not null,
      |number_tinyint tinyint not null,
      |number_smallint smallint not null,
      |number_mediumint mediumint not null,
      |number_int int not null,
      |number_bigint bigint not null,
      |number_decimal decimal(9,6),
      |number_float float,
      |number_double double,
      |primary key (id)
      |)
    """.stripMargin

  val insertTableNumericColumns =
    """
      |insert into numbers (
      |number_tinyint,
      |number_smallint,
      |number_mediumint,
      |number_int,
      |number_bigint,
      |number_decimal,
      |number_float,
      |number_double
      |) values
      |(-100, 32766, 8388607, 2147483647, 9223372036854775807, 450.764491, 14.7, 87650.9876)
    """.stripMargin

  val preparedInsertTableNumericColumns =
    """
      |insert into numbers (
      |number_tinyint,
      |number_smallint,
      |number_mediumint,
      |number_int,
      |number_bigint,
      |number_decimal,
      |number_float,
      |number_double
      |) values
      |(?, ?, ?, ?, ?, ?, ?, ?)
    """.stripMargin

  val createTableTimeColumns =
    """CREATE TEMPORARY TABLE posts (
       id INT NOT NULL AUTO_INCREMENT,
       created_at_date DATE not null,
       created_at_datetime DATETIME not null,
       created_at_timestamp TIMESTAMP not null,
       created_at_time TIME not null,
       created_at_year YEAR not null,
       primary key (id)
      )"""

  val insertTableTimeColumns =
    """
      |insert into posts (created_at_date, created_at_datetime, created_at_timestamp, created_at_time, created_at_year)
      |values ( '2038-01-19', '2013-01-19 03:14:07', '2020-01-19 03:14:07', '03:14:07', '1999' )
    """.stripMargin

  final val createTable = """CREATE TEMPORARY TABLE users (
                              id INT NOT NULL AUTO_INCREMENT ,
                              name VARCHAR(255) CHARACTER SET 'utf8' NOT NULL ,
                              PRIMARY KEY (id) );"""
  final val insert = """INSERT INTO users (name) VALUES ('Maurício Aragão')"""
  final val select = """SELECT * FROM users"""

  def defaultConfiguration = new Configuration(
    "mysql_async",
    "localhost",
    port = 3306,
    password = Some("root"),
    database = Some("mysql_async_tests")
  )

  def withPool[T]( fn : (ConnectionPool[MySQLConnection]) => T ) : T = {

    val factory = new MySQLConnectionFactory(this.defaultConfiguration)
    val pool = new ConnectionPool[MySQLConnection](factory, PoolConfiguration.Default)

    try {
      fn(pool)
    } finally {
      awaitFuture( pool.close )
    }

  }

  def withConfigurablePool[T]( configuration : Configuration )( fn : (ConnectionPool[MySQLConnection]) => T ) : T = {

    val factory = new MySQLConnectionFactory(configuration)
    val pool = new ConnectionPool[MySQLConnection](factory, PoolConfiguration.Default)

    try {
      fn(pool)
    } finally {
      awaitFuture( pool.close )
    }

  }

  def withConnection[T]( fn : (MySQLConnection) => T ) : T =
    withConfigurableConnection(this.defaultConfiguration)(fn)

  def withConfigurableConnection[T]( configuration : Configuration )(fn : (MySQLConnection) => T) : T = {
    val connection = new MySQLConnection(configuration)

    try {
      awaitFuture( connection.connect )
      fn(connection)
    } finally {
      awaitFuture( connection.close )
    }

  }

  def executeQuery( connection : Connection, query : String  ) : QueryResult = {
    awaitFuture( connection.sendQuery(query) )
  }

  def executePreparedStatement( connection : Connection, query : String, values : Any * ) : QueryResult = {
    awaitFuture( connection.sendPreparedStatement(query, values) )
  }

}
