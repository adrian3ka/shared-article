package com.example.evaluation

import com.example.entity.{ActualResult, Model, PredictedResult}
import org.apache.predictionio.controller.{AverageMetric, EmptyEvaluationInfo}


case class Accuracy()
  extends AverageMetric[EmptyEvaluationInfo, Model, PredictedResult, ActualResult] {
  override
  def calculate(query: Model, predicted: PredictedResult, actual: ActualResult)
  : Double = (if (predicted.label == actual.label) 1.0 else 0.0)
}