include(
    ":pool-async",
    ":db-async-common",
    ":mysql-async",
    ":postgresql-async",
    ":r2dbc-mysql"
)

pluginManagement {
    val KOTLIN_VERSION: String by settings
    val BINTRAY_VERSION: String by settings
    val ARTIFACTORY_VERSION: String by settings
    val KTLINT_VERSION: String by settings

    plugins {
        kotlin("jvm") version KOTLIN_VERSION
        id("com.jfrog.bintray") version BINTRAY_VERSION
        id("com.jfrog.artifactory") version ARTIFACTORY_VERSION
        id("org.jlleitschuh.gradle.ktlint") version KTLINT_VERSION
    }
}
