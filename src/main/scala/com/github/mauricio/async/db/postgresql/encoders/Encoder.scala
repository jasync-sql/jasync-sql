package com.github.mauricio.postgresql.encoders

import com.github.mauricio.postgresql.messages.frontend.FrontendMessage
import org.jboss.netty.buffer.ChannelBuffer

/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 7:16 PM
 */

trait Encoder {

  def encode( message : FrontendMessage ) : ChannelBuffer

}
