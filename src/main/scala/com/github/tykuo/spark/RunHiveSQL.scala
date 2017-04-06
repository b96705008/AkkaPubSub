package com.github.tykuo.spark

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession

/**
  * Created by roger19890107 on 05/04/2017.
  */
object RunHiveSQL extends App {
  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("com").setLevel(Level.OFF)
  Logger.getRootLogger.setLevel(Level.OFF)

  var spark: SparkSession = _

  println("args:")
  args.foreach(println)

  if (args.length > 1 && args(0) == "yarn") {
    println("Run on Yarn")
    spark = SparkSession
      .builder()
      .appName("Spark on Hive")
      .master("yarn-client")
      .config("spark.sql.warehouse.dir", "/user/hive/warehouse")
      .config("hive.metastore.uris", args(1))
      .enableHiveSupport()
      .getOrCreate()
  } else {
    println("Run on Local")
    spark = SparkSession
      .builder()
      .appName("Local Spark")
      .master("local[*]")
      .getOrCreate()
  }

  val sql = spark.sql _
  sql("show databases").show()

  println("Start dump data...")
  sql("show databases").write
    .mode("overwrite")
    .parquet("target/outputs/sql-example")

  println("Done")
  spark.close()
}
