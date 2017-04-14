#!/usr/bin/env bash

# export HADOOP_CONF_DIR=...
# export SPARK_HOME=...
# export PATH=$PATH:$SPARK_HOME/bin
export APP_SCRIPTS_HOME=/Users/roger19890107/Developer/main/projects/cathay/hippo/AkkaPubSub/scripts

spark-submit \
    ${APP_SCRIPTS_HOME}/spark.py
