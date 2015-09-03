name := "zookeeper-conf"

version := "1.1b"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
    "io.spray" %% "spray-http" % "1.3.3",
    "io.spray" %% "spray-can" % "1.3.3",
    "io.spray" %% "spray-routing" % "1.3.3",
    "com.typesafe.akka" %% "akka-actor" % "2.3.12",
    "org.apache.curator" % "curator-framework" % "2.8.0",
    "org.slf4j" % "slf4j-simple" % "1.7.12"
)
