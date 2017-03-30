name := "AkkaPubSub"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += Resolver.bintrayRepo("cakesolutions", "maven")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.17",
  "com.typesafe.akka" %% "akka-persistence" % "2.4.17",
  "com.typesafe.akka" %% "akka-remote" % "2.4.17",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.5",
  "org.iq80.leveldb" % "leveldb" % "0.7",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "com.github.etaty" %% "rediscala" % "1.8.0",
  "net.cakesolutions" %% "scala-kafka-client" % "0.10.2.0",
  "net.cakesolutions" %% "scala-kafka-client-akka" % "0.10.2.0",
  "org.apache.spark" % "spark-core_2.11" % "2.1.0",
  "org.apache.spark" % "spark-sql_2.11" % "2.1.0",
  "org.apache.spark" % "spark-mllib_2.11" % "2.1.0",
  "org.apache.spark" % "spark-streaming_2.11" % "2.1.0",
  "org.apache.spark" % "spark-streaming-kafka-0-10_2.11" % "2.1.0",
  "org.apache.bahir" %% "spark-streaming-akka" % "2.1.0"
)