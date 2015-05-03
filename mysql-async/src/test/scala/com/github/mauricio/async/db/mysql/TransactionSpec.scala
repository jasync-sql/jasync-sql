package com.github.mauricio.async.db.mysql

import java.util.UUID
import java.util.concurrent.TimeUnit

import org.specs2.mutable.Specification
import com.github.mauricio.async.db.util.FutureUtils.awaitFuture
import com.github.mauricio.async.db.mysql.exceptions.MySQLException
import com.github.mauricio.async.db.Connection

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Success, Failure}

object TransactionSpec {

  val BrokenInsert = """INSERT INTO users (id, name) VALUES (1, 'Maurício Aragão')"""
  val InsertUser = """INSERT INTO users (name) VALUES (?)"""
  val TransactionInsert = "insert into transaction_test (id) values (?)"

}

class TransactionSpec extends Specification with ConnectionHelper {

  import TransactionSpec._

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
              c.sendQuery(this.insert).flatMap(r => c.sendQuery(BrokenInsert))
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
              c.sendQuery(BrokenInsert)
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

    "runs commands for a transaction in a single connection" in {

      val id = UUID.randomUUID().toString

      withPool {
        pool =>
          val operations = pool.inTransaction {
            connection =>
              connection.sendPreparedStatement(TransactionInsert, List(id)).flatMap {
                result =>
                  connection.sendPreparedStatement(TransactionInsert, List(id)).map {
                    failure =>
                      List(result, failure)
                  }
              }
          }

          Await.ready(operations, Duration(5, TimeUnit.SECONDS))

          operations.value.get match {
            case Success(e) => failure("should not have executed")
            case Failure(e) => {
              e.asInstanceOf[MySQLException].errorMessage.errorCode === 1062
              executePreparedStatement(pool, "select * from transaction_test where id = ?", id).rows.get.size === 0
              success("ok")
            }
          }

      }

    }

  }

}
