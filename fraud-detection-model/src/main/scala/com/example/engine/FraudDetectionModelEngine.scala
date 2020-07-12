package com.example.engine

import com.example.algorithms.naivebayes.NaiveBayesAlgorithm
import com.example.data.{DataSource, Preparator}
import org.apache.predictionio.controller.{Engine, EngineFactory}

object FraudDetectionModelEngine extends EngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("naive" -> classOf[NaiveBayesAlgorithm]),
      classOf[Serving])
  }
}
