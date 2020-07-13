package com.example.data

import grizzled.slf4j.Logger
import org.apache.predictionio.controller.PPreparator
import org.apache.spark.SparkContext

class Preparator extends PPreparator[TrainingData, PreparedData] {
  @transient lazy val logger = Logger[this.type]

  override
  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    logger.info("***************=========== prepare on Preparator executed ===========***************")
    new PreparedData(trainingData.labeledPoints)
  }
}