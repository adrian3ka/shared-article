package com.example.evaluation

import com.example.engine.FraudDetectionModelEngine
import org.apache.predictionio.controller.Evaluation

object AccuracyEvaluation extends Evaluation {
  // Define Engine and Metric used in Evaluation
  engineMetric = (FraudDetectionModelEngine(), Accuracy())
}
