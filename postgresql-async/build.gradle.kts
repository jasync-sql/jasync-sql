val KOTLIN_VERSION: String by project
val KOTLIN_COROUTINES_VERSION: String by project
val SL4J_VERSION: String by project
val JODA_VERSION: String by project
val JODA_CONVERT_VERSION: String by project
val NETTY_VERSION: String by project
val KOTLIN_LOGGING_VERSION: String by project

val JUNIT_VERSION: String by project
val ASSERTJ_VERSION: String by project
val MOCKK_VERSION: String by project
val LOGBACK_VERSION: String by project
val TEST_CONTAINERS_VERSION: String by project
val AWAITILITY_VERSION: String by project

dependencies {
    implementation(project(":db-async-common"))
    implementation(project(":pool-async"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$KOTLIN_VERSION")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$KOTLIN_COROUTINES_VERSION")
    implementation("org.slf4j:slf4j-api:$SL4J_VERSION")
    implementation("joda-time:joda-time:$JODA_VERSION")
    implementation("org.joda:joda-convert:$JODA_CONVERT_VERSION")
    implementation("io.netty:netty-transport:$NETTY_VERSION")
    implementation("io.netty:netty-handler:$NETTY_VERSION")
    implementation("io.github.microutils:kotlin-logging:$KOTLIN_LOGGING_VERSION")
    testImplementation("junit:junit:$JUNIT_VERSION")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$KOTLIN_VERSION")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$KOTLIN_VERSION")
    testImplementation("org.assertj:assertj-core:$ASSERTJ_VERSION")
    testImplementation("io.mockk:mockk:$MOCKK_VERSION")
    testImplementation("ch.qos.logback:logback-classic:$LOGBACK_VERSION")
    testImplementation("org.testcontainers:postgresql:$TEST_CONTAINERS_VERSION")
    testImplementation("org.awaitility:awaitility-kotlin:$AWAITILITY_VERSION")
}
