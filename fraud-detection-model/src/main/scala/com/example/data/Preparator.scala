package com.example.data

import org.apache.predictionio.controller.PPreparator
import org.apache.spark.SparkContext

class Preparator extends PPreparator[TrainingData, PreparedData] {
  override
  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    new PreparedData(trainingData.labeledPoints)
  }
}