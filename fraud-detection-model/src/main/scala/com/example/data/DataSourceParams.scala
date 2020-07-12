package com.example.data

import org.apache.predictionio.controller.Params

case class DataSourceParams
(
  appName: String,
  evalK: Option[Int] // define the k-fold parameter.
) extends Params