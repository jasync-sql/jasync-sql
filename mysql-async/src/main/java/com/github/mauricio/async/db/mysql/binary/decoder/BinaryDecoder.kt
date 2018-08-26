
package com.github.mauricio.async.db.mysql.binary.decoder

import io.netty.buffer.ByteBuf

interface BinaryDecoder {

  fun decode( buffer : ByteBuf ) : Any?

}
