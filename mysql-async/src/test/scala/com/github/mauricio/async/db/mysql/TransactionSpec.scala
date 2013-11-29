package com.github.mauricio.async.db.mysql

import org.specs2.mutable.Specification
import com.github.mauricio.async.db.util.ExecutorServiceUtils._
import com.github.mauricio.async.db.util.FutureUtils.awaitFuture

class TransactionSpec extends Specification with ConnectionHelper {

  val insertUser = """INSERT INTO users (name) VALUES (?)"""

  "connection in transaction" should {

    "correctly store the values of the transaction" in {
      withConnection {
        connection =>
          executeQuery(connection, this.createTable)

          val future = connection.inTransaction {
            c =>
            c.sendPreparedStatement(this.insert)
             .flatMap( r => connection.sendPreparedStatement(this.insert))
          }

          awaitFuture(future)

          val result = executePreparedStatement(connection, this.select).rows.get
          result.size === 2

          result(0)("name") === "Maurício Aragão"
          result(1)("name") === "Maurício Aragão"
      }
    }

  }

}
