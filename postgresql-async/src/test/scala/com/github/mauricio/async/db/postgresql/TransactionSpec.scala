package com.github.mauricio.async.db.postgresql

import org.specs2.mutable.Specification
import com.github.mauricio.async.db.util.Log
import com.github.mauricio.async.db.exceptions.DatabaseException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.Exception.catching

class TransactionSpec extends Specification with DatabaseTestHelper {

  val log = Log.get[TransactionSpec]

  val tableCreate = "CREATE TEMP TABLE transaction_test (x integer PRIMARY KEY)"
  def tableInsert(x : Int) = "INSERT INTO transaction_test VALUES (" + x.toString + ")"
  val tableSelect = "SELECT x FROM transaction_test ORDER BY x"

  "transactions" should {

    "commit simple inserts" in {
      withHandler { handler =>
        executeDdl(handler, tableCreate)
        await(handler.inTransaction { conn =>
          conn.sendQuery(tableInsert(1)).flatMap { _ =>
            conn.sendQuery(tableInsert(2))
          }
        })

        val rows = executeQuery(handler, tableSelect).rows.get
        rows.length === 2
        rows(0)(0) === 1
        rows(1)(0) === 2
      }
    }

    "rollback on error" in {
      withHandler { handler =>
        executeDdl(handler, tableCreate)
        catching(classOf[DatabaseException]).opt(
          await(handler.inTransaction { conn =>
            conn.sendQuery(tableInsert(1)).flatMap { _ =>
              conn.sendQuery(tableInsert(1))
            }
          })
        ) === None

        val rows = executeQuery(handler, tableSelect).rows.get
        rows.length === 0
      }

    }

    "rollback explicitly" in {
      withHandler { handler =>
        executeDdl(handler, tableCreate)
        await(handler.inTransaction { conn =>
          conn.sendQuery(tableInsert(1)).flatMap { _ =>
            conn.sendQuery("ROLLBACK")
          }
        })

        val rows = executeQuery(handler, tableSelect).rows.get
        rows.length === 0
      }

    }

    "rollback to savepoint" in {
      withHandler { handler =>
        executeDdl(handler, tableCreate)
        await(handler.inTransaction { conn =>
          conn.sendQuery(tableInsert(1)).flatMap { _ =>
            conn.sendQuery("SAVEPOINT one").flatMap { _ =>
              conn.sendQuery(tableInsert(2)).flatMap { _ =>
                conn.sendQuery("ROLLBACK TO SAVEPOINT one")
              }
            }
          }
        })

        val rows = executeQuery(handler, tableSelect).rows.get
        rows.length === 1
        rows(0)(0) === 1
      }

    }

  }

}
