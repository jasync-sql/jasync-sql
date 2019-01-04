package com.github.jasync.sql.db.exceptions

import com.github.jasync.sql.db.Connection

@Suppress("RedundantVisibilityModifier")
public class ConnectionNotConnectedException(val connection: Connection) :
    DatabaseException("The connection %s is not connected to the database".format(connection))
