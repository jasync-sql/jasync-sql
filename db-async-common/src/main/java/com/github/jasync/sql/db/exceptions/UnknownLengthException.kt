package com.github.jasync.sql.db.exceptions

@Suppress("RedundantVisibilityModifier")
public class UnknownLengthException(length: Int) : DatabaseException("Can't handle the length $length")
