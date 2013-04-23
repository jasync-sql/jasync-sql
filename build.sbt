name := "postgresql-async"

version := "0.0.1"

organization := "com.github.mauricio"

scalaVersion := "2.10.1"

libraryDependencies ++= Seq(
    "commons-pool" % "commons-pool" % "1.6",
    "ch.qos.logback" % "logback-classic" % "1.0.9",
    "io.netty" % "netty" % "3.6.5.Final",
    "joda-time" % "joda-time" % "2.2",
    "org.joda" % "joda-convert" % "1.3.1",
    "org.specs2" % "specs2_2.10" % "1.14" % "test"
)

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")