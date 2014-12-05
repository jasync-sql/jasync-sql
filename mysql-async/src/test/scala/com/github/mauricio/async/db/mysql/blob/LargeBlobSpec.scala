package com.github.mauricio.async.db.mysql.blob

import java.io.{BufferedOutputStream, File, FileInputStream, FileOutputStream}
import java.nio.ByteBuffer

import com.github.mauricio.async.db.mysql.ConnectionHelper
import io.netty.buffer.Unpooled
import org.specs2.mutable.{After, Specification}
import org.specs2.specification.Scope

class LargeBlobSpec extends Specification with ConnectionHelper {

  val create = """CREATE TEMPORARY TABLE t (
                 |  id BIGINT NOT NULL AUTO_INCREMENT,
                 |  theblob LONGBLOB NOT NULL,
                 |  PRIMARY KEY (id)
                 |);""".stripMargin

  val preparedInsert = "INSERT INTO t (theblob) VALUES (?)"
  // val select = "SELECT theblob FROM t WHERE ID=?"

  "connection" should {

    "handle large BLOBs from InputStream" in new BlobFile {

      withConnection {
        connection =>
          executeQuery(connection, create)

          val stream = new FileInputStream(blobFile)
          executePreparedStatement(connection, preparedInsert, stream)
      }

    }

    "handle BLOBs from ByteBuffer" in new BlobBuffer {

      val preparedInsert = "INSERT INTO t (theblob) VALUES (?)"
      // val select = "SELECT theblob FROM t WHERE ID=?"

      withConnection {
        connection =>
          executeQuery(connection, create)

          executePreparedStatement(connection, preparedInsert, blobBuffer)
      }

    }

    "handle BLOBs from ByteBuf" in new BlobBuf {

      val preparedInsert = "INSERT INTO t (theblob) VALUES (?)"
      // val select = "SELECT theblob FROM t WHERE ID=?"

      withConnection {
        connection =>
          executeQuery(connection, create)

          executePreparedStatement(connection, preparedInsert, blobBuf)
      }

    }

 }

}

trait BlobFile extends After {
  lazy val blobFile = {
    val file = File.createTempFile("blob1", null)
    val bos = new BufferedOutputStream(new FileOutputStream(file))
    0 to ((16 * 1024 * 1024)-1) foreach { n => bos.write(n & 128) }
    bos.close()
    file
  }

  // lazy val outFile = File.createTempFile("blob2", null)

  def after = {
    blobFile.delete()
    // outFile.delete()
  }
}

trait BlobBuffer extends Scope {
  lazy val blobBuffer = {
    val array = new Array[Byte](1024)
    0 to (1024-1) foreach { n => array(n) = (n & 128).asInstanceOf[Byte] }
    ByteBuffer.wrap(array)
  }
}

trait BlobBuf extends Scope {
  lazy val blobBuf = {
    val array = new Array[Byte](1024)
    0 to (1024-1) foreach { n => array(n) = (n & 128).asInstanceOf[Byte] }
    Unpooled.copiedBuffer(array)
  }
}