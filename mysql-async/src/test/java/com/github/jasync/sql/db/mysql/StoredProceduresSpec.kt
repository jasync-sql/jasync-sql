package com.github.jasync.sql.db.mysql


class StoredProceduresSpec : ConnectionHelper() {
/*
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
  */
}
