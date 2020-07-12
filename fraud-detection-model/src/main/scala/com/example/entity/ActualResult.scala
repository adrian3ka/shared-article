package com.example.entity

case class ActualResult
(
  label: Double,
  fraudStatus: String
)

object ActualResult {
  def apply(fraudStatus: FraudStatus): ActualResult = {
    ActualResult(fraudStatus.numericType, fraudStatus.name())
  }
}