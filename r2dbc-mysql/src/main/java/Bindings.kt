package com.github.jasync.r2dbc.mysql

/**
 * Statement parameters bindings.
 */
internal class Bindings {

    private val bindings: MutableList<Map<Int, Any?>> = mutableListOf()

    private var current: MutableMap<Int, Any?>? = null

    fun all(): List<Map<Int, Any?>> = bindings

    fun current(): MutableMap<Int, Any?> {
        var current = this.current

        if (current == null) {
            current = mutableMapOf()
            this.current = current
            this.bindings.add(current)
        }

        return current
    }

    fun done() {
        current = null
    }
}
