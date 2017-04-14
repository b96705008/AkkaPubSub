#!/usr/bin/env bash

# export HADOOP_CONF_DIR=...
# export SPARK_HOME=...
# export PATH=$PATH:$SPARK_HOME/bin
export APP_JAR_HOME=/Users/roger19890107/Developer/main/projects/cathay/hippo/AkkaPubSub/target/scala-2.11

spark-submit \
      --class com.github.tykuo.spark.RunHiveSQL \
      ${APP_JAR_HOME}/AkkaPubSub-assembly-1.0.jar