package com.github.jasync.sql.db.util

import io.netty.buffer.ByteBuf
import java.nio.charset.Charset


fun ByteBuf.readLengthEncodedString(charset: Charset): String = ChannelWrapper.readLengthEncodedString(this, charset)
fun ByteBuf.readBinaryLength(): Long = ChannelWrapper.readBinaryLength(this)
fun ByteBuf.writeLength(length: Long): Unit = ChannelWrapper.writeLength(this, length)
fun ByteBuf.readCString(charset: Charset): String = ChannelWrapper.readCString(this, charset)
fun ByteBuf.readUntilEOF(charset: Charset): String = ChannelWrapper.readUntilEOF(this, charset)
fun ByteBuf.readFixedString(length: Int, charset: Charset): String = ChannelWrapper.readFixedString(this, length, charset)
fun ByteBuf.writeLengthEncodedString(value: String, charset: Charset): Unit = ChannelWrapper.writeLengthEncodedString(this, value, charset)

