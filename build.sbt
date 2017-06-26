name := "august_data_cleanup"

version := "1.0"

scalaVersion := "2.11.11"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.5.15",
  "com.typesafe" % "config" % "1.3.1",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.datastax.cassandra" % "cassandra-driver-core" % "2.0.12",
  "com.google.code.findbugs" % "jsr305" % "3.0.2"
)