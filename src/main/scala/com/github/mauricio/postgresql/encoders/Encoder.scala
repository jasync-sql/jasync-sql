package com.github.mauricio.postgresql.encoders

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.messages.frontend.FrontendMessage

/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 7:16 PM
 */

trait Encoder {

  def encode( message : FrontendMessage ) : ChannelBuffer

}
