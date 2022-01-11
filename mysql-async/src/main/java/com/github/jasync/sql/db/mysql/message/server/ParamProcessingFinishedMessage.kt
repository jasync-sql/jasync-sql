package com.github.jasync.sql.db.mysql.message.server

data class ParamProcessingFinishedMessage(val eofMessage: EOFMessage) :
    ServerMessage(ServerMessage.ParamProcessingFinished)
