import de.johoop.jacoco4sbt._
import JacocoPlugin._

scalaVersion in ThisBuild := "2.10.1"

javacOptions in ThisBuild := Seq("-source", "1.5", "-target", "1.5", "-encoding", "UTF8")

scalacOptions in ThisBuild := Seq("-deprecation", "-unchecked", "-feature", "–encoding", "UTF8", "–explaintypes")

organization in ThisBuild := "com.github.mauricio"

publishArtifact in Test in ThisBuild := false

publishMavenStyle in ThisBuild := true

autoScalaLibrary in ThisBuild := true

pomIncludeRepository := { _ => false }

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

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
      <id>mauricio-linhares</id>
      <name>Maurício Linhares</name>
      <url>https://github.com/mauricio</url>
    </developer>
  </developers>
)

seq(jacoco.settings : _*)