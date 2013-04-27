name := "postgresql-async"

version := "0.0.1-SNAPSHOT"

organization := "com.github.mauricio"

scalaVersion := "2.10.1"

libraryDependencies ++= Seq(
    "commons-pool" % "commons-pool" % "1.6",
    "ch.qos.logback" % "logback-classic" % "1.0.9",
    "io.netty" % "netty" % "3.6.5.Final",
    "joda-time" % "joda-time" % "2.2",
    "org.joda" % "joda-convert" % "1.3.1",
    "org.specs2" %% "specs2" % "1.14" % "test"
)

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>http://your.project.url</url>
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
      <id>mauricio-linhares</id>
      <name>Maurício Linhares</name>
      <url>https://github.com/mauricio</url>
    </developer>
  </developers>
)