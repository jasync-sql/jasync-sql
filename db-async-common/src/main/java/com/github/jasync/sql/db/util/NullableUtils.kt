package com.github.jasync.sql.db.util

internal fun <T : Any, R : Any> T?.nullableMap(mapping: (T) -> R): R? {
    return when {
        this == null -> null
        else -> mapping(this)
    }
}

internal fun <T : Any> T?.nullableFilter(predicate: (T) -> Boolean): T? {
    return when {
        this == null -> null
        predicate(this) -> this
        else -> null
    }
}
