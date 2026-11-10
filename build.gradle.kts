import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.plugins.signing.Sign

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    jacoco
    signing
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            val sonatypeUser = System.getenv("SONATYPE_USERNAME_2")?.takeIf { it.isNotBlank() }
                ?: project.findProperty("sonatypeUsername")?.toString()?.takeIf { it.isNotBlank() }
            val sonatypePass = System.getenv("SONATYPE_PASSWORD_2")?.takeIf { it.isNotEmpty() }
                ?: project.findProperty("sonatypePassword")?.toString()?.takeIf { it.isNotEmpty() }
            sonatypeUser?.let { username.set(it) }
            sonatypePass?.let { password.set(it) }
        }
    }
}

apply(plugin = "io.github.gradle-nexus.publish-plugin")

allprojects {

    group = "com.github.jasync-sql"
    version = "2.2.7"

    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "jacoco")

    repositories {
        mavenCentral()
    }

    tasks {
        compileKotlin {
            kotlinOptions.suppressWarnings = true
            kotlinOptions.jvmTarget = "1.8"
        }

        compileTestKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }

        val JACOCO_VERSION: String by project
        jacoco {
            toolVersion = JACOCO_VERSION
        }

        register<JacocoReport>("codeCoverageReport") {
            dependsOn(test)

            executionData.setFrom(
                fileTree(project.rootDir.absolutePath) {
                    include("**/build/jacoco/*.exec")
                }
            )

            reports {
                xml.isEnabled = true
                xml.destination = file("$buildDir/reports/jacoco/report.xml")
                html.isEnabled = false
                csv.isEnabled = false
            }

            subprojects {
                sourceSets(sourceSets.main.get())
            }
        }

        check {
            dependsOn(ktlintCheck)
        }

        test {
            jvmArgs = listOf(
                "-Dio.netty.leakDetection.level=PARANOID"
            )
        }
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "signing")

    java {
        withSourcesJar()
        withJavadocJar()
    }

    val varintName = when (project.name) {
        "db-async-common" -> "jasync-common"
        "pool-async" -> "jasync-pool"
        "mysql-async" -> "jasync-mysql"
        "postgresql-async" -> "jasync-postgresql"
        "postgis-jasync" -> "jasync-postgis-jts"
        "r2dbc-mysql" -> "jasync-r2dbc-mysql"
        else -> "jasync-sql-unknown"
    }

    tasks {
        jar {
            base.archivesBaseName = varintName
        }
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifactId = varintName

                pom {
                    name.set("jasync-sql")
                    description.set("jasync-sql - Async, Netty based, JVM database drivers for PostgreSQL and MySQL written in Kotlin")
                    url.set("https://github.com/jasync-sql/jasync-sql")

                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            name.set("Ohad Shai")
                            email.set("ohadshai@gmail.com")
                            organization.set("github")
                            organizationUrl.set("http://www.github.com")
                        }
                    }

                    scm {
                        url.set("https://github.com/jasync-sql/jasync-sql/tree/master")
                        connection.set("scm:git:git://github.com/jasync-sql/jasync-sql.git")
                        developerConnection.set("scm:git:ssh://github.com:jasync-sql/jasync-sql.git")
                    }
                }
            }
        }
    }

    signing {
        val gpgPrivateKey = System.getProperty("GPG_PRIVATE_KEY")?.takeIf { it.isNotBlank() }
            ?: System.getenv("GPG_PRIVATE_KEY")?.takeIf { it.isNotBlank() }
        val gpgPrivatePassword = System.getProperty("GPG_PRIVATE_PASSWORD")?.takeIf { it.isNotEmpty() }
            ?: System.getenv("GPG_PRIVATE_PASSWORD")?.takeIf { it.isNotEmpty() }
            ?: ""
        if (gpgPrivateKey != null) {
            useInMemoryPgpKeys(gpgPrivateKey, gpgPrivatePassword)
        }
        sign(publishing.publications["mavenJava"])
    }

    tasks.withType<AbstractPublishToMaven>().configureEach {
        mustRunAfter(tasks.withType<Sign>())
    }
}
