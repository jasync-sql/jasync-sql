package com.github.jasync.sql.db.util

inline fun XXX(reason: String): Nothing =
    throw UnsupportedOperationException("An operation is not implemented: $reason")

val String.size: Int get() = this.length
