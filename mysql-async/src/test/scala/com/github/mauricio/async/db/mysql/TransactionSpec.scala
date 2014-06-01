package com.github.mauricio.async.db.mysql

import org.specs2.mutable.Specification
import com.github.mauricio.async.db.util.FutureUtils.awaitFuture
import com.github.mauricio.async.db.mysql.exceptions.MySQLException
import com.github.mauricio.async.db.Connection

class TransactionSpec extends Specification with ConnectionHelper {

  val brokenInsert = """INSERT INTO users (id, name) VALUES (1, 'Maurício Aragão')"""
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

    "correctly rollback changes if the transaction raises an exception" in {

      withConnection {
        connection =>
          executeQuery(connection, this.createTable)
          executeQuery(connection, this.insert)

          val future = connection.inTransaction {
            c =>
              c.sendQuery(this.insert).flatMap(r => c.sendQuery(brokenInsert))
          }

          try {
            awaitFuture(future)
            failure("should not have arrived here")
          } catch {
            case e : MySQLException => {

              e.errorMessage.errorCode === 1062
              e.errorMessage.errorMessage === "Duplicate entry '1' for key 'PRIMARY'"

              val result = executePreparedStatement(connection, this.select).rows.get
              result.size === 1
              result(0)("name") === "Maurício Aragão"
              success("correct result")
            }
          }
      }

    }

    "should make a connection invalid and not return it to the pool if it raises an exception" in {

      withPool {
        pool =>

          executeQuery(pool, this.createTable)
          executeQuery(pool, this.insert)

          var connection : Connection = null

          val future = pool.inTransaction {
            c =>
              connection = c
              c.sendQuery(this.brokenInsert)
          }

          try {
            awaitFuture(future)
            failure("this should not be reached")
          } catch {
            case e : MySQLException => {

              pool.availables must have size(0)
              pool.availables must not contain(connection.asInstanceOf[MySQLConnection])

              success("success")
            }
          }

      }

    }

  }

}
