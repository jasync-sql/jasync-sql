@file:Suppress("unused")

package com.github.jasync.sql.db.mysql.util

enum class CapabilityFlag(val value: Int) {
    CLIENT_PROTOCOL_41(0x0200),
    CLIENT_CONNECT_WITH_DB(0x0008),
    CLIENT_TRANSACTIONS(0x2000),
    CLIENT_MULTI_RESULTS(0x20000),
    CLIENT_PLUGIN_AUTH(0x00080000),
    CLIENT_SECURE_CONNECTION(0x00008000),
    CLIENT_CONNECT_ATTRS(0x00100000),
    CLIENT_SSL(0x00000800),
}
