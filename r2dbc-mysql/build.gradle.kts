val KOTLIN_VERSION: String by project
val KOTLIN_COROUTINES_VERSION: String by project
val SL4J_VERSION: String by project
val JODA_VERSION: String by project
val JODA_CONVERT_VERSION: String by project
val NETTY_VERSION: String by project
val KOTLIN_LOGGING_VERSION: String by project
val R2DBC_SPI_VERSION: String by project
val REACTOR_CORE_VERSION: String by project

val JUNIT_VERSION: String by project
val ASSERTJ_VERSION: String by project
val MOCKK_VERSION: String by project
val LOGBACK_VERSION: String by project
val TEST_CONTAINERS_VERSION: String by project
val MYSQL_CONNECTOR_VERSION: String by project
val AWAITILITY_VERSION: String by project

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://repo.spring.io/milestone")
    }
}

dependencies {
    compile(project(":db-async-common"))
    compile(project(":pool-async"))
    compile(project(":mysql-async"))
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$KOTLIN_VERSION")
    compile("org.jetbrains.kotlin:kotlin-reflect:$KOTLIN_VERSION")
    compile("io.r2dbc:r2dbc-spi:$R2DBC_SPI_VERSION")
    implementation("io.projectreactor:reactor-core:$REACTOR_CORE_VERSION")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.1.7")
    compile("org.slf4j:slf4j-api:$SL4J_VERSION")
    compile("joda-time:joda-time:$JODA_VERSION")
    compile("org.joda:joda-convert:$JODA_CONVERT_VERSION")
    compile("io.netty:netty-transport:$NETTY_VERSION")
    compile("io.netty:netty-handler:$NETTY_VERSION")
    compile("io.github.microutils:kotlin-logging:$KOTLIN_LOGGING_VERSION")
    testImplementation("junit:junit:$JUNIT_VERSION")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$KOTLIN_VERSION")
    testImplementation("org.assertj:assertj-core:$ASSERTJ_VERSION")
    testImplementation("io.mockk:mockk:$MOCKK_VERSION")
    testImplementation("ch.qos.logback:logback-classic:$LOGBACK_VERSION")
    testImplementation("org.testcontainers:mysql:$TEST_CONTAINERS_VERSION")
    testImplementation("mysql:mysql-connector-java:$MYSQL_CONNECTOR_VERSION")
    testImplementation("org.awaitility:awaitility-kotlin:$AWAITILITY_VERSION")
}
