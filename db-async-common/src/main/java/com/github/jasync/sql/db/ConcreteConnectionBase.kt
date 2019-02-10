package com.github.jasync.sql.db

import mu.KotlinLogging
import java.util.concurrent.Executor

private val logger = KotlinLogging.logger {}

abstract class ConcreteConnectionBase(
    val configuration: Configuration,
    val executionContext: Executor
) : ConcreteConnection {
}
