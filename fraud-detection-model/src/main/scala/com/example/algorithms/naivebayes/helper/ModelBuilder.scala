package com.example.algorithms.naivebayes.helper

import com.example.entity.Model
import com.example.entity.constant.Attributes
import grizzled.slf4j.Logger
import org.apache.spark.mllib.linalg.{Vector, Vectors}

object ModelBuilder {
  @transient lazy val logger = Logger[this.type]

  def buildVectorFromFraudEntity(model: Model): Vector = {
    val normalizedGtv = model.gtv / Attributes.GTV_NORMALIZER
    val transactionPerUnit = normalizedGtv / model.transactionVelocity
    val avgRelatedAccount: Double = model.relatedAccount.toDouble / model.transactionVelocity.toDouble

    val vector: Vector = Vectors.dense(
      Array(
        model.transactionVelocity,
        normalizedGtv,
        transactionPerUnit,
        model.accountAge,
        model.relatedAccount,
        avgRelatedAccount,
        model.cardType.numericType
      )
    )
    logger.info(s"==========================================")
    logger.info("Successfully built vector:")
    logger.info(s">> Transaction Velocity: ${model.transactionVelocity}")
    logger.info(s">> Normalized GTV: ${normalizedGtv}")
    logger.info(s">> Transaction Per Unit: ${transactionPerUnit}")
    logger.info(s">> Account Age: ${model.accountAge}")
    logger.info(s">> Related Account: ${model.relatedAccount}")
    logger.info(s">> Avg Related Account: ${avgRelatedAccount}")
    logger.info(s">> Card Type: ${model.cardType}")
    logger.info(s"==========================================")

    vector
  }
}
