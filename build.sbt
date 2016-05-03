name := "akka_streaming"

version := "1.0"

scalaVersion := "2.11.7"

val akkaVersion = "2.4.4"

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion
)

libraryDependencies ++= akkaDependencies