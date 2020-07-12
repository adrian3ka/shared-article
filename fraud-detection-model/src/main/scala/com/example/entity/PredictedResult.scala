package com.example.entity

case class PredictedResult
(
  label: Double,
  fraudStatus: String
)

object PredictedResult {
  def apply(fraudStatus: FraudStatus): PredictedResult = {
    PredictedResult(fraudStatus.numericType, fraudStatus.name())
  }
}