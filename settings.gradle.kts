include(
    ":pool-async",
    ":db-async-common",
    ":mysql-async",
    ":postgresql-async",
    ":r2dbc-mysql"
)

pluginManagement {
    val KOTLIN_VERSION: String by settings
    val KTLINT_VERSION: String by settings

    plugins {
        kotlin("jvm") version KOTLIN_VERSION
        id("org.jlleitschuh.gradle.ktlint") version KTLINT_VERSION
    }
}
