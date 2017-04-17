package com.github.tykuo.app


import spray.json._

case class FrontierMessage(db: String,
                           table: String,
                           method: String,
                           partitions: Array[String],
                           partitions_name: String,
                           exec_date: Long)

case class JobFinishMessage(hippo_name: String,
                            job_name: String,
                            success: Boolean,
                            finish_time: Long)


object HippoJsonProtocol extends DefaultJsonProtocol {
  implicit val frontierFormat = jsonFormat6(FrontierMessage)
  implicit val jobFinishFormat = jsonFormat4(JobFinishMessage)
}


object HippoUtils {
  def currentTimestamp: Long = System.currentTimeMillis() / 1000
}

