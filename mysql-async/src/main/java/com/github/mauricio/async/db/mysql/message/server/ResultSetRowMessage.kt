
package com.github.mauricio.async.db.mysql.message.server

import com.github.jasync.sql.db.util.length
import io.netty.buffer.ByteBuf

class ResultSetRowMessage
  : ServerMessage( ServerMessage.Row )
  //, mutable.Buffer<ByteBuf>
{

  private val buffer = mutableListOf<ByteBuf?>()

  fun length(): Int = buffer.length

  //fun apply(idx: Int): ByteBuf = buffer[idx]

//  fun update(n: Int, newelem: ByteBuf) {
//    buffer[n] = newelem
//  }

  fun add(elem: ByteBuf?): ResultSetRowMessage {
    this.buffer.add(elem)
    return this
  }

  fun clear() {
    this.buffer.clear()
  }

  fun addToBegin(elem: ByteBuf): ResultSetRowMessage {
    this.buffer.add(0, elem)
    return this
  }

  fun insertAll(n: Int, elems: List<ByteBuf>) {
    this.buffer.addAll(n, elems)
  }

  fun remove(n: Int): ByteBuf? {
    return this.buffer.removeAt(n)
  }

  fun iterator(): Iterator<ByteBuf?> = this.buffer.iterator()

}
