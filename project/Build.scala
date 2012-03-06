/**
 * User: Maurício Linhares
 * Date: 2/18/12
 * Time: 6:34 PM
 */

import sbt._
import Keys._

object Build extends sbt.Build {

  lazy val project = Project(
    "postgresql-netty",
    file(".")
  ).settings(
      organization := "org.postgresql",
      version := "1.0.0",
      scalaVersion := "2.9.1",
      scalacOptions := Seq("-deprecation", "-encoding", "utf8"),
      resolvers ++= Configuration.resolutionRepos,
      libraryDependencies ++= Configuration.dependencies)

}


object Configuration {

  object Repos {
    val JavaNet = "java-net-maven" at "http://download.java.net/maven/2"
    val SonatypeSnapshots = "sonatype-snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
    val SonatypeReleases = "sonatype-releases" at "http://oss.sonatype.org/content/repositories/releases"
    val SpringSource = "spring-source" at "http://repository.springsource.com/ivy/bundles/release"
    val SpringExternal = "spring-external" at "http://repository.springsource.com/maven/bundles/external"
  }

  val resolutionRepos = Seq[Resolver](
    ScalaToolsReleases,
    ScalaToolsSnapshots,
    Repos.SonatypeReleases,
    Repos.SonatypeSnapshots,
    Repos.SpringExternal,
    Repos.SpringSource,
    Repos.JavaNet
  )

  val dependencies = Seq(
    // main dependencies
    "io.netty" % "netty" % "3.3.1.Final",
    "commons-pool" % "commons-pool" % "1.6",
    "joda-time" % "joda-time" % "2.0",
    "org.joda" % "joda-convert" % "1.2",
    "ch.qos.logback" % "logback-classic" % "1.0.0",
    "org.scala-lang" % "scala-compiler" % "2.9.1",
    "org.scala-lang" % "scala-library" % "2.9.1",

    // test dependencies
    "org.specs2" % "specs2_2.9.1" % "1.8.1" % "test"
  )

}