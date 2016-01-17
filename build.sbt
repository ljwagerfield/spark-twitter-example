name := "spark-twitter-example"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-core_2.11" % "1.6.0" % "provided",
  "org.apache.spark" % "spark-streaming_2.11" % "1.6.0" % "provided",
  ("org.apache.spark" % "spark-streaming-twitter_2.11" % "1.6.0").exclude("org.spark-project.spark", "unused")
)

javaOptions += "-Dlog4j.configuration=log4j2.xml"
