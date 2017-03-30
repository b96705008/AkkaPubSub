package com.github.tykuo.mongo


import org.apache.hadoop.conf.Configuration
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import org.bson.BSONObject
import com.mongodb.hadoop.{BSONFileInputFormat, BSONFileOutputFormat, MongoInputFormat, MongoOutputFormat}
import com.mongodb.hadoop.io.MongoUpdateWritable
import org.apache.log4j.{Level, Logger}


/**
  * # If using auth, the db user should be assigned the auth to do "splitVector" action
  * # https://docs.mongodb.com/v3.2/reference/built-in-roles/#cluster-administration-roles
  */
object RunMongoHadoop extends App {

  def getAuthURI(collection: String) =
    // mongodb://localhost:27017/mongo-spark.spark-output
    s"mongodb://spark:spark@localhost:27017/mongo-spark.$collection?authSource=admin"

  // mongo config
  val mongoConf = new Configuration()
  mongoConf.set("mongo.input.uri", getAuthURI("spark-input"))

  // config
  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("com").setLevel(Level.OFF)
  Logger.getRootLogger.setLevel(Level.OFF)

  // spark
  val conf = new SparkConf()
    .setMaster("local[*]")
    .setAppName("MongoHadoop")
  val sc = new SparkContext(conf)

  // Mongo Read
  val docs = sc.newAPIHadoopRDD(
    mongoConf,
    classOf[MongoInputFormat],
    classOf[Object],
    classOf[BSONObject])

  docs.take(10).foreach(println)

  // Mongo write
  val outputConf = new Configuration()
  outputConf.set("mongo.output.uri", getAuthURI("spark-output"))

  docs.saveAsNewAPIHadoopFile(
    "file:///this-is-completely-unused",
    classOf[Object],
    classOf[BSONObject],
    classOf[MongoOutputFormat[Object, BSONObject]],
    outputConf)
}
