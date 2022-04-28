package com.github.aysnc.sql.db.integration

import com.github.jasync.sql.db.postgresql.codec.PostgreSQLConnectionDelegate
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationMessage
import com.github.jasync.sql.db.postgresql.messages.backend.CommandCompleteMessage
import com.github.jasync.sql.db.postgresql.messages.backend.DataRowMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ErrorMessage
import com.github.jasync.sql.db.postgresql.messages.backend.NotificationResponse
import com.github.jasync.sql.db.postgresql.messages.backend.ParameterStatusMessage
import com.github.jasync.sql.db.postgresql.messages.backend.RowDescriptionMessage

class PostgreSQLSlowConnectionDelegate(
    private val delegate: PostgreSQLConnectionDelegate,
    private val onReadyForQuerySlowdownInMillis: Long
) : PostgreSQLConnectionDelegate {
    override fun onAuthenticationResponse(message: AuthenticationMessage) =
        delegate.onAuthenticationResponse(message)

    override fun onCommandComplete(message: CommandCompleteMessage) =
        delegate.onCommandComplete(message)

    override fun onCloseComplete() =
        delegate.onCloseComplete()

    override fun onDataRow(message: DataRowMessage) =
        delegate.onDataRow(message)

    override fun onError(message: ErrorMessage) =
        delegate.onError(message)

    override fun onError(throwable: Throwable) =
        delegate.onError(throwable)

    override fun onParameterStatus(message: ParameterStatusMessage) =
        delegate.onParameterStatus(message)

    override fun onReadyForQuery() {
        Thread.sleep(onReadyForQuerySlowdownInMillis)
        delegate.onReadyForQuery()
    }

    override fun onRowDescription(message: RowDescriptionMessage) =
        delegate.onRowDescription(message)

    override fun onNotificationResponse(message: NotificationResponse) =
        delegate.onNotificationResponse(message)
}
