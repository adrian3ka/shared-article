package com.example.engine

import com.example.entity.constant.Attributes
import com.example.entity.{FraudStatus, Model, PredictedResult}
import grizzled.slf4j.Logger
import org.apache.predictionio.controller.LServing

class Serving extends LServing[Model, PredictedResult] {
  @transient lazy val logger = Logger[this.type]

  override
  def serve(model: Model, predictedResults: Seq[PredictedResult]): PredictedResult = {
    logger.info("Predicted Result >> " + predictedResults)
    var fraudStatus: FraudStatus = FraudStatus.convertFromLiteral(predictedResults.head.fraudStatus)
    var predictionIsModified: Boolean = false

    if (fraudStatus.eq(FraudStatus.SAFE) && model.gtv > Attributes.TRANSACTION_AMOUNT_THRESHOLD) {
      logger.info("Modified Result to Suspicious")
      fraudStatus = FraudStatus.SUSPICIOUS
      predictionIsModified = true
    }

    PredictedResult(fraudStatus, predictionIsModified)
  }
}
