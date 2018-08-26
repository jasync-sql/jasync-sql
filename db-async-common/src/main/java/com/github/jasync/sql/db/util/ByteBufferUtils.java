package com.github.jasync.sql.db.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class ByteBufferUtils {

  public static void writeLength(ByteBuf buffer) {

    int length = buffer.writerIndex() - 1;
    buffer.markWriterIndex();
    buffer.writerIndex(1);
    buffer.writeInt(length);

    buffer.resetWriterIndex();

  }

  public static void writeCString(String content, ByteBuf b, Charset charset) {
    b.writeBytes(content.getBytes(charset));
    b.writeByte(0);
  }

  public static void writeSizedString(String content, ByteBuf b, Charset charset) {
    byte[] bytes = content.getBytes(charset);
    b.writeByte(bytes.length);
    b.writeBytes(bytes);
  }

  public static String readCString(ByteBuf b, Charset charset) {
    b.markReaderIndex();

    byte b1 = 0;
    int count = 0;

    do {
      b1 = b.readByte();
      count += 1;
    } while (b1 != 0);

    b.resetReaderIndex();

    String result = b.toString(b.readerIndex(), count - 1, charset);

    b.readerIndex(b.readerIndex() + count);

    return result;
  }

  public static String readUntilEOF(ByteBuf b, Charset charset) {
    if (b.readableBytes() == 0) {
      return "";
    }

    b.markReaderIndex();

    byte b1 = -1;
    int count = 0;
    int offset = 1;

    while (b1 != 0) {
      if (b.readableBytes() > 0) {
        b1 = b.readByte();
        count += 1;
      } else {
        b1 = 0;
        offset = 0;
      }
    }

    b.resetReaderIndex();

    String result = b.toString(b.readerIndex(), count - offset, charset);

    b.readerIndex(b.readerIndex() + count);

    return result;
  }

  public static int read3BytesInt(ByteBuf b) {
    return (b.readByte() & 0xff) | ((b.readByte() & 0xff) << 8) | ((b.readByte() & 0xff) << 16);
  }

  public static void write3BytesInt(ByteBuf b, int value) {
    b.writeByte(value & 0xff);
    b.writeByte(value >>> 8);
    b.writeByte(value >>> 16);
  }

  //sequence =1
  public static void writePacketLength(ByteBuf buffer, int sequence) {
    int length = buffer.writerIndex() - 4;
    buffer.markWriterIndex();
    buffer.writerIndex(0);

    write3BytesInt(buffer, length);
    buffer.writeByte(sequence);

    buffer.resetWriterIndex();
  }

  public static ByteBuf packetBuffer() {
    return packetBuffer(1024);
  }

  public static ByteBuf packetBuffer(int estimate) {
    ByteBuf buffer = mysqlBuffer(estimate);

    buffer.writeInt(0);

    return buffer;
  }

  public static ByteBuf mysqlBuffer() {
    return mysqlBuffer(1024);
  }

  public static ByteBuf mysqlBuffer(int estimate) {
    return Unpooled.buffer(estimate).order(ByteOrder.LITTLE_ENDIAN);
  }

}
