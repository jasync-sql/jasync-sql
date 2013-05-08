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

import org.specs2.mutable.Specification
import com.github.mauricio.async.db.mysql.exceptions.MySQLException
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.buffer.ChannelBuffers

class QuerySpec extends Specification with ConnectionHelper {

  final val createTable = """CREATE TEMPORARY TABLE users (
                              id INT NOT NULL AUTO_INCREMENT ,
                              name VARCHAR(255) CHARACTER SET 'utf8' NOT NULL ,
                              PRIMARY KEY (id) );"""
  final val insert = """INSERT INTO users (name) VALUES ('Maurício Aragão')"""
  final val select = """SELECT * FROM users"""

  "connection" should {

    "be able to run a DML query" in {

      withConnection {
         connection =>
          executeQuery( connection, this.createTable ).rowsAffected === 0
      }

    }

    "raise an exception upon a bad statement" in {
      withConnection {
        connection =>
          executeQuery(connection, "this is not SQL") must throwA[MySQLException].like {
            case e => e.asInstanceOf[MySQLException].errorMessage.sqlState === "#42000"
          }
      }
    }

    "be able to select from a table" in {

      withConnection {
        connection =>
          executeQuery(connection, this.createTable).rowsAffected === 0
          executeQuery(connection, this.insert).rowsAffected === 1
          val result = executeQuery(connection, this.select).rows.get

          result(0)("id") === 1
          result(0)("name") === "Maurício Linhares"

      }

    }

  }

}
