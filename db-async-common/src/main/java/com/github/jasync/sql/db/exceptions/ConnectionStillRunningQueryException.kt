package com.github.jasync.sql.db.exceptions

@Suppress("RedundantVisibilityModifier")
public class ConnectionStillRunningQueryException(connectionId: String, caughtRace: Boolean) : DatabaseException(
    "<%s> - There is a query still being run here - race -> %s".format(
        connectionId,
        caughtRace
    )
)
