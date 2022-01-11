package com.github.jasync.sql.db.exceptions

@Suppress("RedundantVisibilityModifier")
public class ParserNotAvailableException(t: Byte) :
    DatabaseException("There is no parser available for message type '$t' (${t.toString(16)})")
