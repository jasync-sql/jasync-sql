import com.github.jasync.sql.db.mysql.codec.MySQLFrameDecoder
import com.github.jasync.sql.db.util.ByteBufferUtils
import io.netty.buffer.ByteBuf
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.util.CharsetUtil
import org.junit.Test

class MySQLFrameDecoderSpec  {

  final val charset = CharsetUtil.UTF_8
/*

  @Test
  fun `decode an OK message correctly` () {
      val buffer = createOkPacket()
      val decoder = this.createPipeline()
      decoder.writeInbound(buffer)

      val ok = decoder.readInbound().asInstanceOf[OkMessage]
      traok.affectedRows === 10
      ok.lastInsertId === 15
      ok.message === "this is a test"
      ok.statusFlags === 5
      ok.warnings === 6
  }

   @Test
   fun `decode an error message` () {
      val content = "this is the error message"
      val buffer = createErrorPacket(content)
      val decoder = createPipeline()

      decoder.writeInbound(buffer)

      val error = decoder.readInbound().asInstanceOf[ErrorMessage]

      error.errorCode === 27
      error.errorMessage === content
      error.sqlState === "HZAWAY"

    }

  @Test
  fun `on a query process it should correctly send an OK`  () {
      val decoder = MySQLFrameDecoder(charset, "[mysql-connection]")
      decoder.hasDoneHandshake = true
      val embedder = EmbeddedChannel(decoder)
      embedder.config.setAllocator(LittleEndianByteBufAllocator.INSTANCE)

      decoder.queryProcessStarted()

      decoder.isInQuery must beTrue
      decoder.processingColumns must beTrue

      val buffer = createOkPacket()

      embedder.writeInbound(buffer) must beTrue
      embedder.readInbound().asInstanceOf[OkMessage].message === "this is a test"

      decoder.isInQuery must beFalse
      decoder.processingColumns must beFalse
    }

  @Test
  fun  `on query process it should correctly send an error` () {

      val decoder = new MySQLFrameDecoder(charset, "[mysql-connection]")
      decoder.hasDoneHandshake = true
      val embedder = new EmbeddedChannel(decoder)
      embedder.config.setAllocator(LittleEndianByteBufAllocator.INSTANCE)

      decoder.queryProcessStarted()

      decoder.isInQuery must beTrue
      decoder.processingColumns must beTrue

      val content = "this is a crazy error"

      val buffer = createErrorPacket(content)

      embedder.writeInbound(buffer) must beTrue
      embedder.readInbound().asInstanceOf[ErrorMessage].errorMessage === content

      decoder.isInQuery must beFalse
      decoder.processingColumns must beFalse

    }


  @Test
  fun `on query process it should correctly handle a result set` () {

      val decoder = new MySQLFrameDecoder(charset, "[mysql-connection]")
      decoder.hasDoneHandshake = true
      val embedder = new EmbeddedChannel(decoder)
      embedder.config.setAllocator(LittleEndianByteBufAllocator.INSTANCE)

      decoder.queryProcessStarted()

      decoder.totalColumns === 0

      val columnCountBuffer = ByteBufferUtils.packetBuffer()
      columnCountBuffer.writeLength(2)
      columnCountBuffer.writePacketLength()

      embedder.writeInbound(columnCountBuffer)

      decoder.totalColumns === 2

      val columnId = createColumnPacket("id", ColumnTypes.FIELD_TYPE_LONG)
      val columnName = createColumnPacket("name", ColumnTypes.FIELD_TYPE_VARCHAR)

      embedder.writeInbound(columnId)

      embedder.readInbound().asInstanceOf[ColumnDefinitionMessage].name === "id"

      decoder.processedColumns === 1

      embedder.writeInbound(columnName)

      embedder.readInbound().asInstanceOf[ColumnDefinitionMessage].name === "name"

      decoder.processedColumns === 2

      embedder.writeInbound(this.createEOFPacket())

      embedder.readInbound().asInstanceOf[ColumnProcessingFinishedMessage].eofMessage.flags === 8765

      decoder.processingColumns must beFalse

      val row = ByteBufferUtils.packetBuffer()
      row.writeLenghtEncodedString("1", charset)
      row.writeLenghtEncodedString("some name", charset)
      row.writePacketLength()

      embedder.writeInbound(row)

      embedder.readInbound().isInstanceOf[ResultSetRowMessage] must beTrue

      embedder.writeInbound(this.createEOFPacket())

      decoder.isInQuery must beFalse
    }


  fun createPipeline(): EmbeddedChannel {
    val decoder = new MySQLFrameDecoder(charset, "[mysql-connection]")
    decoder.hasDoneHandshake = true
    val channel = new EmbeddedChannel(decoder)
    channel.config.setAllocator(LittleEndianByteBufAllocator.INSTANCE)
    channel
  }

  fun createOkPacket() : ByteBuf  {
    val buffer = ByteBufferUtils.packetBuffer()
    buffer.writeByte(0)
    buffer.writeLength(10)
    buffer.writeLength(15)
    buffer.writeShort(5)
    buffer.writeShort(6)
    buffer.writeBytes("this is a test".getBytes(charset))
    buffer.writePacketLength()
    buffer
  }

  fun createErrorPacket(content : String) : ByteBuf  {
    val buffer = ByteBufferUtils.packetBuffer()
    buffer.writeByte(0xff)
    buffer.writeShort(27)
    buffer.writeByte("H".toByte().toInt())
    buffer.writeBytes("ZAWAY".getBytes(charset))
    buffer.writeBytes(content.getBytes(charset))
    buffer.writePacketLength()
    return buffer
  }

  fun createColumnPacket( name : String, columnType : Int ) : ByteBuf {
    val buffer = ByteBufferUtils.packetBuffer()
    buffer.writeLenghtEncodedString("def", charset)
    buffer.writeLenghtEncodedString("some_schema", charset)
    buffer.writeLenghtEncodedString("some_table", charset)
    buffer.writeLenghtEncodedString("some_table", charset)
    buffer.writeLenghtEncodedString(name, charset)
    buffer.writeLenghtEncodedString(name, charset)
    ByteBufferUtils.writeLength(buffer)
    buffer.writeShort(0x03)
    buffer.writeInt(10)
    buffer.writeByte(columnType)
    buffer.writeShort(76)
    buffer.writeByte(0)
    buffer.writeShort(56)
    buffer.writePacketLength()
    return buffer
  }

  fun createEOFPacket() : ByteBuf {
    val buffer = ByteBufferUtils.packetBuffer()
    buffer.writeByte(0xfe)
    buffer.writeShort(879)
    buffer.writeShort(8765)

   ByteBufferUtils.writePacketLength(buffer, 0)

    return buffer
  }
*/
}
