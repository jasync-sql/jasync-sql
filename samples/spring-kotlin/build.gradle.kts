import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

group = "spring-kotlin-jasync-sql"
version = "1.0-SNAPSHOT"

val springBootVersion: String by extra

plugins {
    application
    kotlin("jvm") version "1.2.70"
    kotlin("plugin.spring").version("1.2.70")
    id("org.springframework.boot").version("1.5.9.RELEASE")
    id("io.spring.dependency-management") version "1.0.5.RELEASE"
}


buildscript {

    val springBootVersion: String by extra { "2.0.0.M7" }

    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
    }

    repositories {
        mavenCentral()
    }
}

extra["kotlin.version"] = plugins.getPlugin(KotlinPluginWrapper::class.java).kotlinPluginVersion

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("org.jetbrains.kotlin:kotlin-reflect")
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compile("com.github.jasync-sql:jasync-mysql:0.8.32")
    compile("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion")
    compile("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
