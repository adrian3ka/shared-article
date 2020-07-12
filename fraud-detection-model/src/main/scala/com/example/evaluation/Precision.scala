package com.example.evaluation

import com.example.entity.{ActualResult, Model, PredictedResult}
import org.apache.predictionio.controller.{EmptyEvaluationInfo, OptionAverageMetric}

case class Precision(label: Double)
  extends OptionAverageMetric[EmptyEvaluationInfo, Model, PredictedResult, ActualResult] {
  override def header: String = s"Precision(label = $label)"

  override
  def calculate(query: Model, predicted: PredictedResult
                , actual: ActualResult)
  : Option[Double] = {
    if (predicted.label == label) {
      if (predicted.label == actual.label) {
        Some(1.0) // True positive
      } else {
        Some(0.0) // False positive
      }
    } else {
      None // Unrelated case for calculating precision
    }
  }
}
