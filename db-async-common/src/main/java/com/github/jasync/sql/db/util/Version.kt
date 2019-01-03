package com.github.jasync.sql.db.util

//reusing KotlinVersion as it has the same semantics
typealias Version = KotlinVersion

fun parseVersion(version: String): Version {
    @Suppress("SpellCheckingInspection")
    val splitted = version.split(".")

    return Version(
        (if (splitted.isNotEmpty()) splitted[0].toIntOrNull() else null) ?: 0,
        (if (splitted.size > 1) splitted[1].toIntOrNull() else null) ?: 0,
        (if (splitted.size > 2) splitted[2].toIntOrNull() else null) ?: 0
    )
}
