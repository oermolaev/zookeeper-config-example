name := "zookeeper-conf"

version := "1.2"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
    "io.spray" %% "spray-http" % "1.3.4",
    "io.spray" %% "spray-can" % "1.3.4",
    "io.spray" %% "spray-routing" % "1.3.4",
    "com.typesafe.akka" %% "akka-actor" % "2.3.16",
    "org.apache.curator" % "curator-framework" % "2.9.1",
    "org.slf4j" % "slf4j-simple" % "1.7.13"
)
