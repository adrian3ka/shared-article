package com.example.engine

import com.example.entity.{Model, PredictedResult}
import grizzled.slf4j.Logger
import org.apache.predictionio.controller.LServing

class Serving extends LServing[Model, PredictedResult] {
  @transient lazy val logger = Logger[this.type]

  override
  def serve(query: Model, predictedResults: Seq[PredictedResult]): PredictedResult = {

    logger.info("Predicted Result >> " + predictedResults)
    predictedResults.head
  }
}
