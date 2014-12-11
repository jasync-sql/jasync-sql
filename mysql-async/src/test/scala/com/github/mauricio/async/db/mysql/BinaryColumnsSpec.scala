package com.github.mauricio.async.db.mysql

import java.io.{FileInputStream, FileOutputStream, BufferedOutputStream, File}

import org.specs2.mutable.{After, Specification}
import java.util.UUID
import java.nio.ByteBuffer
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import com.github.mauricio.async.db.RowData

class BinaryColumnsSpec extends Specification with ConnectionHelper {

  val createBlobTable =
    """CREATE TEMPORARY TABLE POSTS (
      | id INT NOT NULL,
      | blob_column LONGBLOB,
      | primary key (id))
    """.stripMargin

  val insertIntoBlobTable = "INSERT INTO POSTS (id,blob_column) VALUES (?,?)"

  val selectFromBlobTable = "SELECT id,blob_column FROM POSTS ORDER BY id"

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

    "support BLOB type with long values" in {

      val bytes = (1 to 2100).map(_.toByte).toArray

      testBlob(bytes)

    }

    "support BLOB type with ScatteringByteChannel input" in new BlobFile {

      withConnection {
        connection =>
          executeQuery(connection, createBlobTable)

          val channel = new FileInputStream(blobFile).getChannel
          executePreparedStatement(connection, insertIntoBlobTable, 1, channel)

          val Some(rows) = executeQuery(connection, selectFromBlobTable).rows
          rows(0)("id") === 1
          val retrievedBlob = rows(0)("blob_column").asInstanceOf[Array[Byte]]
          retrievedBlob.length === BlobSize
          0 to retrievedBlob.length-1 foreach { n => retrievedBlob(n) === n.toByte }
      }

    }

  }

  def testBlob(bytes: Array[Byte]) = {
    withConnection {
      connection =>
        executeQuery(connection, createBlobTable)
        executePreparedStatement(connection, insertIntoBlobTable, 1, Some(bytes))
        executePreparedStatement(connection, insertIntoBlobTable, 2, ByteBuffer.wrap(bytes))
        executePreparedStatement(connection, insertIntoBlobTable, 3, Unpooled.wrappedBuffer(bytes))

        val Some(rows) = executeQuery(connection, selectFromBlobTable).rows
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

trait BlobFile extends After {
  val BlobSize = (16 * 1024 * 1024)-9

  lazy val blobFile = {
    val file = File.createTempFile("blob", null)
    val bos = new BufferedOutputStream(new FileOutputStream(file))
    0 to BlobSize-1 foreach { n => bos.write(n.toByte) }
    bos.close()
    file
  }

  def after = {
    blobFile.delete()
  }
}
