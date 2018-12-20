package com.github.jasync.sql.db.exceptions

/**
 * Thrown to indicate that a URL Parser could not understand the provided URL.
 */
@Suppress("RedundantVisibilityModifier")
public class UnableToParseURLException(message: String, base: Throwable?) : RuntimeException(message, base) {
    constructor(message: String) : this(message, null)
}
