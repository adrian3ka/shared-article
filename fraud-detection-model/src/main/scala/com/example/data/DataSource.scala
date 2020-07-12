package com.example.data

import com.example.data.helper.DataSourceHelper
import com.example.entity.{ActualResult, CardType, FraudStatus, Model}
import grizzled.slf4j.Logger
import org.apache.predictionio.controller.{EmptyEvaluationInfo, PDataSource}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD


class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData, EmptyEvaluationInfo, Model, ActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val labeledPoints: RDD[LabeledPoint] = DataSourceHelper.aggregateDataSource(dsp, sc)

    new TrainingData(labeledPoints)
  }

  override
  def readEval(sc: SparkContext)
  : Seq[(TrainingData, EmptyEvaluationInfo, RDD[(Model, ActualResult)])] = {
    require(dsp.evalK.nonEmpty, "DataSourceParams.evalK must not be None")

    val labeledPoints: RDD[LabeledPoint] = DataSourceHelper.aggregateDataSource(dsp, sc)

    // K-fold splitting
    val evalK = dsp.evalK.get
    val indexedPoints: RDD[(LabeledPoint, Long)] = labeledPoints.zipWithIndex()

    (0 until evalK).map { idx =>
      System.out.println("Current DataSource Index: " + idx)

      val trainingPoints = indexedPoints.filter(_._2 % evalK != idx).map(_._1)
      val testingPoints = indexedPoints.filter(_._2 % evalK == idx).map(_._1)

      System.out.println("trainingPoints count >> ", trainingPoints.count())
      System.out.println("trainingPoints       >> ", trainingPoints)
      System.out.println("testingPoints  count >> ", testingPoints.count())
      System.out.println("testingPoints        >> ", testingPoints)

      (
        new TrainingData(trainingPoints),
        new EmptyEvaluationInfo(),
        testingPoints.map {
          p =>
            (Model(
              p.features(0).toInt,
              p.features(1),
              p.features(2).toInt,
              p.features(3).toInt,
              CardType.convertFromNumeric(p.features(4).toInt)
            ), ActualResult(FraudStatus.convertFromNumeric(p.label.toInt)))
        }
      )
    }
  }
}