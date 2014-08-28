name := """akka-ladybugs-spray-ws"""

version := "1.0"

scalaVersion := "2.10.4"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.1.6" % "test",
  "com.wandoulabs.akka" %% "spray-websocket" % "0.1.2",
  "org.webjars" % "jquery" % "2.1.1"
)

Revolver.settings
