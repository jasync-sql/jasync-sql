import sbt._
import Keys._

object ProjectBuild extends Build {

  val commonName = "db-async-common"
  val postgresqlName = "postgresql-async"
  val mysqlName = "mysql-async"

  lazy val root = Project(
    id = "db-async-base",
    base = file("."),
    settings = Configuration.baseSettings,
    aggregate = Seq(common, postgresql, mysql)
  )

  lazy val common = Project(
    id = commonName,
    base = file(commonName),
    settings = Configuration.baseSettings ++ Seq(
      name := commonName,
      libraryDependencies ++= Configuration.commonDependencies
    )
  )

  lazy val postgresql = Project(
    id = postgresqlName,
    base = file(postgresqlName),
    settings = Configuration.baseSettings ++ Seq(
      name := postgresqlName,
      libraryDependencies ++= Configuration.implementationDependencies
    )
  ) dependsOn (common)

  lazy val mysql = Project(
    id = mysqlName,
    base = file(mysqlName),
    settings = Configuration.baseSettings ++ Seq(
      name := mysqlName,
      libraryDependencies ++= Configuration.implementationDependencies
    )
  ) dependsOn (common)

}

object Configuration {

  val commonVersion = "0.2.17-SNAPSHOT"
  val projectScalaVersion = "2.11.0"

  val specs2Dependency = "org.specs2" %% "specs2" % "2.3.11" % "test"
  val logbackDependency = "ch.qos.logback" % "logback-classic" % "1.0.13" % "test"

  val commonDependencies = Seq(
    "org.slf4j" % "slf4j-api" % "1.7.5",
    "joda-time" % "joda-time" % "2.3",
    "org.joda" % "joda-convert" % "1.5",
    "io.netty" % "netty-all" % "4.0.25.Final",
    "org.javassist" % "javassist" % "3.18.1-GA",
    specs2Dependency,
    logbackDependency
  )

  val implementationDependencies = Seq(
    specs2Dependency,
    logbackDependency
  )

  val baseSettings = Defaults.defaultSettings ++ Seq(
    scalacOptions :=
      Opts.compile.encoding("UTF8")
        :+ Opts.compile.deprecation
        :+ Opts.compile.unchecked
        :+ "-feature"
    ,
    scalacOptions in doc := Seq("-doc-external-doc:scala=http://www.scala-lang.org/archives/downloads/distrib/files/nightly/docs/library/"),
    crossScalaVersions := Seq(projectScalaVersion, "2.10.4"),
    javacOptions := Seq("-source", "1.6", "-target", "1.6", "-encoding", "UTF8"),
    organization := "com.github.mauricio",
    version := commonVersion,
    parallelExecution := false,
    publishArtifact in Test := false,
    publishMavenStyle := true,
    pomIncludeRepository := {
      _ => false
    },
    publishTo <<= version {
      v: String =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    pomExtra := (
      <url>https://github.com/mauricio/postgresql-async</url>
        <licenses>
          <license>
            <name>APACHE-2.0</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:mauricio/postgresql-netty.git</url>
          <connection>scm:git:git@github.com:mauricio/postgresql-netty.git</connection>
        </scm>
        <developers>
          <developer>
            <id>mauricio</id>
            <name>Maurício Linhares</name>
            <url>https://github.com/mauricio</url>
          </developer>
        </developers>
      )
  )

}
