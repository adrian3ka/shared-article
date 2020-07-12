package com.example.evaluation

import com.example.algorithms.naivebayes.AlgorithmParams
import com.example.data.DataSourceParams
import org.apache.predictionio.controller.{EngineParams, EngineParamsGenerator}

object EngineParamsList extends EngineParamsGenerator {
  // Define list of EngineParams used in Evaluation

  // First, we define the base engine params. It specifies the appId from which
  // the data is read, and a evalK parameter is used to define the
  // cross-validation.
  private[this] val baseEP = EngineParams(
    dataSourceParams = DataSourceParams(appName = "INVALID_APP_NAME", evalK = Some(5)))

  // Second, we specify the engine params list by explicitly listing all
  // algorithm parameters. In this case, we evaluate 3 engine params, each with
  // a different algorithm params value.
  engineParamsList = Seq(
    baseEP.copy(algorithmParamsList = Seq(("naive", AlgorithmParams(10.0)))),
    baseEP.copy(algorithmParamsList = Seq(("naive", AlgorithmParams(100.0)))),
    baseEP.copy(algorithmParamsList = Seq(("naive", AlgorithmParams(1000.0)))))
}
