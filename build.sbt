import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

name := """akka-actor-ladybugs"""

version := "1.0"

scalaVersion := "2.11.6"

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka"   %% "akka-actor"       % "2.3.9" withSources(),
  "com.typesafe.akka"   %% "akka-slf4j"       % "2.3.9",
  "io.spray"            %% "spray-json"       % "1.3.1",
  "com.wandoulabs.akka" %% "spray-websocket"  % "0.1.4",
  "org.webjars"         %  "normalize.css"    % "3.0.2",
  "org.webjars"         %  "jquery"           % "2.1.1",
  "org.webjars"         %  "lodash"           % "2.4.1-6",
  "org.scalatest"       %% "scalatest"        % "2.2.4" % "test"
)

Revolver.settings

packageArchetype.java_application
