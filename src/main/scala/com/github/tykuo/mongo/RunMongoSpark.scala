package com.github.tykuo.mongo

import com.mongodb.spark.config.ReadConfig
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession


/**
  * # If using auth, the db user should be assigned the auth to do "splitVector" action
  * # MongoDB version should greater or equal to 3.2
  * # https://docs.mongodb.com/v3.2/reference/built-in-roles/#cluster-administration-roles
  */
object RunMongoSpark extends App {
  // config
  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("com").setLevel(Level.OFF)
  Logger.getRootLogger.setLevel(Level.OFF)

  import com.mongodb.spark._

  import org.bson.Document

  def getAuthURI(collection: String) =
    s"mongodb://spark:spark@localhost:27017/mongo-spark.$collection?authSource=admin"

  val spark = SparkSession.builder()
      .master("local[*]")
      .appName("SparkMongo")
      .config("spark.mongodb.input.uri", getAuthURI("spark-input"))
      .config("spark.mongodb.output.uri", getAuthURI("spark-output"))
      .getOrCreate()

  val df = MongoSpark.load(spark).drop("_id")
  df.printSchema()

  println("Start write df to spark-output...")
  MongoSpark.save(df)
}
