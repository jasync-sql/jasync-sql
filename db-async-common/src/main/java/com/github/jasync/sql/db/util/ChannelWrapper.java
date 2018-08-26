package com.github.jasync.sql.db.util;

import com.github.jasync.sql.db.exceptions.UnknownLengthException;
import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

public class ChannelWrapper {
  //  implicit public void bufferToWrapper( buffer :ByteBuf) = new ChannelWrapper(buffer)
//
  private static final short MySQL_NULL = 0xfb;
//  final val log = Log.get[ChannelWrapper]

  public static String readFixedString(ByteBuf buffer, int length, Charset charset) {
    byte[] bytes = new byte[length];
    buffer.readBytes(bytes);
    return new String(bytes, charset);
  }

  public static String readCString(ByteBuf buffer, Charset charset) {
    return ByteBufferUtils.readCString(buffer, charset);
  }

  public static String readUntilEOF(ByteBuf buffer, Charset charset) {
    return ByteBufferUtils.readUntilEOF(buffer, charset);
  }

  public static String readLengthEncodedString(ByteBuf buffer, Charset charset) {
    long length = readBinaryLength(buffer);
    return readFixedString(buffer, (int) length, charset);
  }

  public static long readBinaryLength(ByteBuf buffer) {
    short firstByte = buffer.readUnsignedByte();


    if (firstByte <= 250) {
      return firstByte;
    } else {
      switch (firstByte) {
        case MySQL_NULL:
          return -1;
        case 252:
          return buffer.readUnsignedShort();
        case 253:
          return readLongInt(buffer);
        case 254:
          return buffer.readLong();
        default:
          throw new UnknownLengthException(firstByte);
      }
    }

  }

  public static int readLongInt(ByteBuf buffer) {
    byte first = buffer.readByte();
    byte second = buffer.readByte();
    byte third = buffer.readByte();

    return (first & 0xff) | ((second & 0xff) << 8) | ((third & 0xff) << 16);
  }

  public static void writeLength(ByteBuf buffer, long length) {
    if (length < 251) {
      buffer.writeByte((byte) length);
    } else if (length < 65536L) {
      buffer.writeByte(252);
      buffer.writeShort((int) length);
    } else if (length < 16777216L) {
      buffer.writeByte(253);
      writeLongInt(buffer, (int) length);
    } else {
      buffer.writeByte(254);
      buffer.writeLong(length);
    }
  }

  public static void writeLongInt(ByteBuf buffer, int i) {
    buffer.writeByte(i & 0xff);
    buffer.writeByte(i >>> 8);
    buffer.writeByte(i >>> 16);
  }

  public static void writeLenghtEncodedString(ByteBuf buffer, String value, Charset charset) {
    byte[] bytes = value.getBytes(charset);
    writeLength(buffer, bytes.length);
    buffer.writeBytes(bytes);
  }

  // : Int = 0
  public static void writePacketLength(ByteBuf buffer, int sequence) {
    ByteBufferUtils.writePacketLength(buffer, sequence);
  }

  public static int mysqlReadInt(ByteBuf buffer) {
    byte first = buffer.readByte();
    byte last = buffer.readByte();

    return (first & 0xff) | ((last & 0xff) << 8);
  }

}
