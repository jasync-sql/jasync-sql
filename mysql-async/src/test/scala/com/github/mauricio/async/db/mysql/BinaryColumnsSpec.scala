package com.github.mauricio.async.db.mysql

import org.specs2.mutable.Specification
import java.util.UUID
import java.nio.ByteBuffer
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import com.github.mauricio.async.db.RowData

class BinaryColumnsSpec extends Specification with ConnectionHelper {

  "connection" should {

    "correctly load fields as byte arrays" in {

      val create = """CREATE TEMPORARY TABLE t (
                     |  id BIGINT NOT NULL AUTO_INCREMENT,
                     |  uuid BINARY(36) NOT NULL,
                     |  address VARBINARY(16) NOT NULL,
                     |  PRIMARY KEY (id),
                     |  INDEX idx_t_uuid (uuid),
                     |  INDEX idx_t_address (address)
                     |);""".stripMargin

      val uuid = UUID.randomUUID().toString
      val host = "127.0.0.1"

      val preparedInsert = "INSERT INTO t (uuid, address) VALUES (?, ?)"
      val insert = s"INSERT INTO t (uuid, address) VALUES ('${uuid}', '${host}')"
      val select = "SELECT * FROM t"

      withConnection {
        connection =>
          executeQuery(connection, create)
          executeQuery(connection, insert)

          val result = executeQuery(connection, select).rows.get

          compareBytes(result(0), "uuid", uuid )
          compareBytes(result(0), "address", host )

          executePreparedStatement( connection, preparedInsert, uuid, host)

          val otherResult = executePreparedStatement(connection, select).rows.get

          compareBytes(otherResult(1), "uuid", uuid )
          compareBytes(otherResult(1), "address", host )
      }

    }

    "support BINARY type" in {

      val create =
        """CREATE TEMPORARY TABLE POSTS (
          | id INT NOT NULL AUTO_INCREMENT,
          | binary_column BINARY(20),
          | primary key (id))
        """.stripMargin

      val insert = "INSERT INTO POSTS (binary_column) VALUES (?)"
      val select = "SELECT * FROM POSTS"
      val bytes = (1 to 10).map(_.toByte).toArray
      val padding = Array.fill[Byte](10)(0)

      withConnection {
        connection =>
          executeQuery(connection, create)
          executePreparedStatement(connection, insert, bytes)
          val row = executeQuery(connection, select).rows.get(0)
          row("id") === 1
          row("binary_column") === bytes ++ padding
      }

    }

    "support VARBINARY type" in {

      val create =
        """CREATE TEMPORARY TABLE POSTS (
          | id INT NOT NULL AUTO_INCREMENT,
          | varbinary_column VARBINARY(20),
          | primary key (id))
        """.stripMargin

      val insert = "INSERT INTO POSTS (varbinary_column) VALUES (?)"
      val select = "SELECT * FROM POSTS"
      val bytes = (1 to 10).map(_.toByte).toArray

      withConnection {
        connection =>
          executeQuery(connection, create)
          executePreparedStatement(connection, insert, bytes)
          val row = executeQuery(connection, select).rows.get(0)
          row("id") === 1
          row("varbinary_column") === bytes
      }

    }

    "support BLOB type" in {

      val bytes = (1 to 10).map(_.toByte).toArray

      testBlob(bytes)

    }

    "support BLOB type with large values" in {

      val bytes = (1 to 2100).map(_.toByte).toArray

      testBlob(bytes)

    }

  }

  def testBlob(bytes: Array[Byte]) = {
    val create =
      """CREATE TEMPORARY TABLE POSTS (
        | id INT NOT NULL,
        | blob_column BLOB,
        | primary key (id))
      """.stripMargin

    val insert = "INSERT INTO POSTS (id,blob_column) VALUES (?,?)"
    val select = "SELECT id,blob_column FROM POSTS ORDER BY id"

    withConnection {
      connection =>
        executeQuery(connection, create)
        executePreparedStatement(connection, insert, 1, Some(bytes))
        executePreparedStatement(connection, insert, 2, ByteBuffer.wrap(bytes))
        executePreparedStatement(connection, insert, 3, Unpooled.wrappedBuffer(bytes))

        val Some(rows) = executeQuery(connection, select).rows
        rows(0)("id") === 1
        rows(0)("blob_column") === bytes
        rows(1)("id") === 2
        rows(1)("blob_column") === bytes
        rows(2)("id") === 3
        rows(2)("blob_column") === bytes
        rows.size === 3
    }

  }

  def compareBytes( row : RowData, column : String, expected : String ) =
    row(column) === expected.getBytes(CharsetUtil.UTF_8)

}