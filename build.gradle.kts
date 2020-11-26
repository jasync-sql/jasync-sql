import com.jfrog.bintray.gradle.BintrayExtension.GpgConfig
import com.jfrog.bintray.gradle.BintrayExtension.MavenCentralSyncConfig
import com.jfrog.bintray.gradle.BintrayExtension.PackageConfig
import com.jfrog.bintray.gradle.BintrayExtension.VersionConfig
import java.util.Date

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

plugins {
    kotlin("jvm")
    id("com.jfrog.bintray")
    id("com.jfrog.artifactory")
    id("org.jlleitschuh.gradle.ktlint")
    `maven-publish`
    jacoco
}

allprojects {
    group = "com.github.jasync-sql"
    version = "1.1.5" + (if (System.getProperty("snapshot")?.toBoolean() == true) "-SNAPSHOT" else "")

    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "jacoco")

    repositories {
        mavenCentral()
        jcenter()
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

            executionData.setFrom(fileTree(project.rootDir.absolutePath) {
                include("**/build/jacoco/*.exec")
            })

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
    apply(plugin = "com.jfrog.bintray")
    apply(plugin = "com.jfrog.artifactory")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    java {
        withSourcesJar()
        withJavadocJar()
    }

    sourceSets.main {
        java.srcDirs("src/main/java")
    }

    val varintName = when (project.name) {
        "db-async-common" -> "jasync-common"
        "pool-async" -> "jasync-pool"
        "mysql-async" -> "jasync-mysql"
        "postgresql-async" -> "jasync-postgresql"
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
            create<MavenPublication>("mavenProject") {
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

    bintray {
        user = System.getProperty("bintray.user")
        key = System.getProperty("bintray.key") // https://bintray.com/profile/edit
        setPublications("mavenProject")
        dryRun = false // [Default: false] Whether to run this as dry-run, without deploying
        publish = true // [Default: false] Whether version should be auto published after an upload
        pkg(closureOf<PackageConfig> {
            repo = "jasync-sql"
            name = "jasync-sql"
            userOrg = "jasync-sql"
            setLicenses("Apache-2.0")
            vcsUrl = "https://github.com/jasync-sql/jasync-sql.git"
            websiteUrl = "https://github.com/jasync-sql/jasync-sql"
            issueTrackerUrl = "https://github.com/jasync-sql/jasync-sql/issues"
            publicDownloadNumbers = true
            githubRepo = "jasync-sql/jasync-sql"
            githubReleaseNotesFile = "CHANGELOG.md"
            version(closureOf<VersionConfig> {
                name = project.version.toString()
                desc = "jasync-sql - Async, Netty based, JVM database drivers for PostgreSQL and MySQL written in Kotlin"
                released = Date().toString()
                gpg(closureOf<GpgConfig> {
                    sign = true // Determines whether to GPG sign the files. The default is false
                })
                mavenCentralSync(closureOf<MavenCentralSyncConfig> {
                    sync = true // [Default: true] Determines whether to sync the version to Maven Central.
                    user = System.getProperty("maven.user") // OSS user token: mandatory
                    password = System.getProperty("maven.password") // OSS user password: mandatory
                    close = "1" // Optional property. By default the staging repository is closed and artifacts are released to Maven Central. You can optionally turn this behaviour off (by puting 0 as value) and release the version manually.
                })
            })
        })
    }

    artifactory {
        setContextUrl("http://oss.jfrog.org")
        publish(delegateClosureOf<org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig> {
            repository(delegateClosureOf<groovy.lang.GroovyObject> {
                setProperty("repoKey", "oss-snapshot-local")
                setProperty("username", System.getProperty("bintray.user"))
                setProperty("password", System.getProperty("bintray.key"))
                setProperty("maven", true)
            })
            defaults(delegateClosureOf<groovy.lang.GroovyObject> {
                invokeMethod("publications", publishing.publications.names.toTypedArray())
                setProperty("publishArtifacts", true)
                setProperty("publishPom", true)
            })
        })
        resolve(delegateClosureOf<org.jfrog.gradle.plugin.artifactory.dsl.ResolverConfig> {
            setProperty("repoKey", "jcenter")
        })
        clientConfig.info.setBuildNumber(System.getProperty("build.number"))
    }
}
