package com.github.jasync.sql.db.mysql.encoder

import com.github.jasync.sql.db.exceptions.UnsupportedAuthenticationMethodException
import com.github.jasync.sql.db.mysql.encoder.auth.AuthenticationMethod
import com.github.jasync.sql.db.mysql.message.client.AuthenticationSwitchResponse
import com.github.jasync.sql.db.mysql.message.client.ClientMessage
import com.github.jasync.sql.db.util.ByteBufferUtils
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class AuthenticationSwitchResponseEncoder(val charset: Charset) : MessageEncoder {

    override fun encode(message: ClientMessage): ByteBuf {
        val switch = message as AuthenticationSwitchResponse

        val method = switch.request.method
        val authenticator = AuthenticationMethod.Availables.getOrElse(method) {
            throw UnsupportedAuthenticationMethodException(method)
        }

        val buffer = ByteBufferUtils.packetBuffer()

        val bytes = authenticator.generateAuthentication(
            charset,
            switch.password,
            switch.request.seed,
            switch.sslConfiguration,
            switch.rsaPublicKey,
        )
        buffer.writeBytes(bytes)

        return buffer
    }
}
