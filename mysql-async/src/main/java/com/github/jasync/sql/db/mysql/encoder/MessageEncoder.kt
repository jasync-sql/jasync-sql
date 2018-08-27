
package com.github.jasync.sql.db.mysql.encoder

import io.netty.buffer.ByteBuf
import com.github.jasync.sql.db.mysql.message.client.ClientMessage

interface MessageEncoder {

  fun encode( message : ClientMessage ) : ByteBuf

}
