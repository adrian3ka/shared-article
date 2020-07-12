package com.example.algorithms.naivebayes

import com.example.algorithms.naivebayes.helper.ModelBuilder
import com.example.algorithms.naivebayes.helper.ModelBuilder.logger
import com.example.data.PreparedData
import com.example.entity.constant.Attributes
import com.example.entity.{FraudStatus, Model, PredictedResult}
import grizzled.slf4j.Logger
import org.apache.predictionio.controller.P2LAlgorithm
import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.{NaiveBayes, NaiveBayesModel}
import org.apache.spark.mllib.linalg.Vectors

// extends P2LAlgorithm because the MLlib's NaiveBayesModel doesn't contain RDD.
class NaiveBayesAlgorithm(val ap: AlgorithmParams)
  extends P2LAlgorithm[PreparedData, NaiveBayesModel, Model, PredictedResult] {

  @transient lazy val logger: Logger = Logger[this.type]

  override
  def train(sc: SparkContext, data: PreparedData): NaiveBayesModel = {
    // MLLib NaiveBayes cannot handle empty training data.
    require(data.labeledPoints.take(1).nonEmpty,
      s"RDD[labeledPoints] in PreparedData cannot be empty." +
        " Please check if DataSource generates TrainingData" +
        " and Preparator generates PreparedData correctly.")

    NaiveBayes.train(data.labeledPoints, ap.lambda)
  }

  override
  def predict(model: NaiveBayesModel, query: Model): PredictedResult = {

    logger.info("---------------------------------------------")
    logger.info("Predicting Model: ")
    logger.info(s"Transaction Velocity: ${query.transactionVelocity}")
    logger.info(s"GTV: ${query.gtv}")
    logger.info(s"Account Age: ${query.accountAge}")
    logger.info(s"Related Account: ${query.relatedAccount}")
    logger.info(s"Card Type: ${query.cardType}")
    logger.info("---------------------------------------------")

    val label = model.predict(ModelBuilder.buildVectorFromFraudEntity(query))

    val fraudStatus = FraudStatus.convertFromNumeric(label.toInt)

    PredictedResult(fraudStatus)
  }
}
