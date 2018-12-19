package com.github.jasync.sql.db.exceptions

import io.netty.channel.ChannelFuture

class CanceledChannelFutureException(val channelFuture: ChannelFuture) :
    IllegalStateException("This channel future was canceled -> %s".format(channelFuture))
