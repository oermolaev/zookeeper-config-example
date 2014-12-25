import sbtassembly.Plugin.assemblySettings

name := "zookeeper-conf"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
    "io.spray" %% "spray-http" % "1.3.2",
    "io.spray" %% "spray-can" % "1.3.2",
    "io.spray" %% "spray-routing" % "1.3.2",
    "com.typesafe.akka" %% "akka-actor" % "2.3.8",
    "org.apache.curator" % "curator-framework" % "2.7.0",
    "org.slf4j" % "slf4j-simple" % "1.7.9"
)

assemblySettings
