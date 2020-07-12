package com.example.data

import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD

class PreparedData
(
  val labeledPoints: RDD[LabeledPoint]
) extends Serializable