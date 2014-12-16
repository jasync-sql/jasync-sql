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

import com.github.mauricio.async.db.ResultSet
import com.github.mauricio.async.db.util.FutureUtils._
import org.specs2.mutable.Specification

class StoredProceduresSpec extends Specification with ConnectionHelper {

  "connection" should {

    "be able to execute create stored procedure" in {
      withConnection {
        connection =>
          val future = for(
            drop <- connection.sendQuery("DROP PROCEDURE IF exists helloWorld;");
            create <- connection.sendQuery(
              """
               CREATE PROCEDURE helloWorld(OUT param1 VARCHAR(20))
               BEGIN
                 SELECT 'hello' INTO param1;
               END
              """
            )
          ) yield create
          awaitFuture(future).statusMessage === ""
      }
    }

    "be able to call stored procedure" in {
      withConnection {
        connection =>
          val future = for(
            drop <- connection.sendQuery("DROP PROCEDURE IF exists constTest;");
            create <- connection.sendQuery(
              """
               CREATE PROCEDURE constTest(OUT param INT)
               BEGIN
                 SELECT 125 INTO param;
               END
              """
            );
            call <- connection.sendQuery("CALL constTest(@arg)");
            arg <- connection.sendQuery("SELECT @arg")
          ) yield arg
          val result: Option[ResultSet] = awaitFuture(future).rows
          result.isDefined === true
          val rows = result.get
          rows.size === 1
          rows(0)(rows.columnNames.head) === 125
      }
    }

    "be able to call stored procedure with input parameter" in {
      withConnection {
        connection =>
          val future = for(
            drop <- connection.sendQuery("DROP PROCEDURE IF exists addTest;");
            create <- connection.sendQuery(
              """
               CREATE PROCEDURE addTest(IN a INT, IN b INT, OUT sum INT)
               BEGIN
                 SELECT a+b INTO sum;
               END
              """
            );
            call <- connection.sendQuery("CALL addTest(132, 245, @sm)");
            res <- connection.sendQuery("SELECT @sm")
          ) yield res
          val result: Option[ResultSet] = awaitFuture(future).rows
          result.isDefined === true
          val rows = result.get
          rows.size === 1
          rows(0)(rows.columnNames.head) === 377
      }
    }

    "be able to remove stored procedure" in {
      withConnection {
        connection =>
          val createResult: Option[ResultSet] = awaitFuture(
            for(
              drop <- connection.sendQuery("DROP PROCEDURE IF exists remTest;");
              create <- connection.sendQuery(
                """
                  CREATE PROCEDURE remTest(OUT cnst INT)
                     BEGIN
                       SELECT 987 INTO cnst;
                     END
                """
              );
              routine <- connection.sendQuery(
                """
                  SELECT routine_name FROM INFORMATION_SCHEMA.ROUTINES WHERE routine_name="remTest"
                """
              )
            ) yield routine
          ).rows
          createResult.isDefined === true
          createResult.get.size === 1
          createResult.get(0)("routine_name") === "remTest"
          val removeResult: Option[ResultSet] = awaitFuture(
            for(
              drop <- connection.sendQuery("DROP PROCEDURE remTest;");
              routine <- connection.sendQuery(
                """
                  SELECT routine_name FROM INFORMATION_SCHEMA.ROUTINES WHERE routine_name="remTest"
                """
              )
            ) yield routine
          ).rows
          removeResult.isDefined === true
          removeResult.get.isEmpty === true
      }
    }
  }
}