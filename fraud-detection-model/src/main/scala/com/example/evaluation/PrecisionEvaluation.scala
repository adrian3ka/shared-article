package com.example.evaluation

import com.example.engine.FraudDetectionModelEngine
import org.apache.predictionio.controller.Evaluation

object PrecisionEvaluation extends Evaluation {
  engineMetric = (FraudDetectionModelEngine(), Precision(label = 1.0))
}
