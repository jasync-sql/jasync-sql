
package com.github.jasync.sql.db.mysql.decoder

import io.netty.buffer.ByteBuf
import com.github.jasync.sql.db.mysql.message.server.ServerMessage

interface MessageDecoder {

   fun decode( buffer : ByteBuf ) : ServerMessage

}
