package com.github.mauricio.postgresql.messages.backend

import org.jboss.netty.buffer.ChannelBuffer

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 1:10 AM
 */
case class DataRowMessage( val values : Array[ChannelBuffer] ) extends Message( Message.DataRow )