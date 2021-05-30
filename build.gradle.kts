
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.15.1")
    }
}

apply(plugin = "com.vanniktech.maven.publish")

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    jacoco
}

allprojects {

    apply(plugin = "kotlin")
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
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    java {
        withSourcesJar()
        withJavadocJar()
    }

    sourceSets.main {
        java.srcDirs("src/main/java")
    }
}
