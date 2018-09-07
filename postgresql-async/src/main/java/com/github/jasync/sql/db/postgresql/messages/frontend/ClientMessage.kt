package com.github.jasync.sql.db.postgresql.messages.frontend

import com.github.jasync.sql.db.KindedMessage

open class ClientMessage(override val kind: Int) : KindedMessage
