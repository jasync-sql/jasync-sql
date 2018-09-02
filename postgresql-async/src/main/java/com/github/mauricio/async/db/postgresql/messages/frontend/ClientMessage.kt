package com.github.mauricio.async.db.postgresql.messages.frontend

import com.github.jasync.sql.db.KindedMessage

open class ClientMessage(override val kind: Int) : KindedMessage