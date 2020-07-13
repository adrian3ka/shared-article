package com.example.entity

case class PredictedResult
(
  label: Double,
  fraudStatus: String,
  predictionIsModified: Boolean
)

object PredictedResult {
  def apply(fraudStatus: FraudStatus): PredictedResult = {
    PredictedResult(fraudStatus.numericType, fraudStatus.name(), false)
  }

  def apply(fraudStatus: FraudStatus, predictionIsModified: Boolean): PredictedResult = {
    PredictedResult(fraudStatus.numericType, fraudStatus.name(), predictionIsModified)
  }
}