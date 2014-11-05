import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

name := """reactive-ladybugs"""

version := "1.0"

scalaVersion := "2.11.2"

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.1.6" % "test",
  "com.wandoulabs.akka" %% "spray-websocket" % "0.1.3",
  "io.spray" %%  "spray-json" % "1.2.6",
  "org.webjars" % "jquery" % "2.1.1",
  "org.webjars" % "lodash" % "2.4.1-6"
)

Revolver.settings

packageArchetype.java_application
