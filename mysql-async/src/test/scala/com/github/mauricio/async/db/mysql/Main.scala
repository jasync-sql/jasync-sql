package com.github.mauricio.async.db.mysql

import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.buffer.ChannelBuffers
import java.nio.charset.Charset

object Main {

  def main(args: Array[String]) {

    val name = "Maurício Aragão".getBytes(CharsetUtil.ISO_8859_1)
    val result = MySQLHelper.dumpAsHex(ChannelBuffers.wrappedBuffer(name), name.length)

    println(result)
  }

}
