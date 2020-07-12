package com.example.data.helper

import com.example.algorithms.naivebayes.helper.ModelBuilder
import com.example.data.DataSourceParams
import com.example.entity.{CardType, FraudStatus, Model}
import com.example.entity.constant.Attributes.{ACCOUNT_AGE, CARD_TYPE, ENTITY_TYPE, FRAUD_STATUS, GTV, RELATED_ACCOUNT, TRANSACTION_VELOCITY, GTV_NORMALIZER}
import grizzled.slf4j.Logger
import org.apache.predictionio.data.store.PEventStore
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD

object DataSourceHelper {
  @transient lazy val logger = Logger[this.type]

  def aggregateDataSource(dsp: DataSourceParams, sc: SparkContext): RDD[LabeledPoint] = {
    return PEventStore.aggregateProperties(
      appName = dsp.appName,
      entityType = ENTITY_TYPE,
      // only keep entities with these required properties defined
      required = Some(List(
        ACCOUNT_AGE,
        CARD_TYPE,
        FRAUD_STATUS,
        GTV,
        RELATED_ACCOUNT,
        TRANSACTION_VELOCITY
      )))(sc)
      // aggregateProperties() returns RDD pair of
      // entity ID and its aggregated properties
      .map { case (entityId, properties) =>
        try {
          val fraudStatus: FraudStatus = FraudStatus.convertFromLiteral(properties.get[String](FRAUD_STATUS))
          val cardType: CardType = CardType.convertFromLiteral(properties.get[String](CARD_TYPE))
          val transactionVelocity: Int = properties.get[Int](TRANSACTION_VELOCITY);
          val gtv: Int = properties.get[Int](GTV);
          val relatedAccount: Int = properties.get[Int](RELATED_ACCOUNT);
          val accountAge: Int = properties.get[Int](ACCOUNT_AGE);

          val model: Model = Model(transactionVelocity, gtv, relatedAccount, accountAge, cardType)

          logger.info(s"Processing $entityId with fraudStatus ($cardType): $fraudStatus")

          LabeledPoint(fraudStatus.numericType.toDouble,
            ModelBuilder.buildVectorFromFraudEntity(model))
        } catch {
          case e: Exception => logger.error(s"Failed to get properties $properties of" +
            s" $entityId. Exception: $e.")
            throw e
        }
      }.cache()
  }
}
