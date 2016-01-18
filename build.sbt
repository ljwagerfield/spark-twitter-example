name := "spark-twitter-example"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-core_2.11" % "1.6.0" % "provided",
  "org.apache.spark" % "spark-streaming_2.11" % "1.6.0" % "provided",
  "org.apache.spark" % "spark-streaming-twitter_2.11" % "1.6.0",
  "org.apache.spark" % "spark-mllib_2.11" % "1.6.0"
)

assemblyMergeStrategy in assembly := {
  case PathList("javax", "xml", xs @ _*) => MergeStrategy.last
  case PathList("org", "apache", xs @ _*) => MergeStrategy.last
  case PathList("com", "google", xs @ _*) => MergeStrategy.last
  case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.last
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
