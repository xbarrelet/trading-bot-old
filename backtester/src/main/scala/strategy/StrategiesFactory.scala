package ch.xavier
package strategy

import signals.Signal

import akka.NotUsed
import akka.stream.scaladsl.Source
import org.ta4j.core.BarSeries

import scala.::
import scala.collection.mutable.ListBuffer
import ch.xavier.strategy.concrete.{
  LeveragedSS, LeveragedTL, LeveragedSSLL, LeveragedSS3TSL,
  LeveragedSS3T, LeveragedSS3TTL, LeveragedSS3TTL2
}

object StrategiesFactory {

  def getAllStrategiesVariantsNames(strategyNames: List[String]): List[String] =
    var names: List[String] = List()

    for (strategyName <- strategyNames) do
      names = names ++ getStrategieVariantsName(strategyName)

    names

  def getStrategieVariantsName(strategyName: String): List[String] =
    val strategiesList: ListBuffer[String] = ListBuffer()

    strategyName match {
      case "SimpleStrategy" =>
        strategiesList += "LeveragedSimpleStrategy_with_leverage_1"
      case "SimpleStrategyWithThreeTargets" =>
        strategiesList += "LeveragedSimpleStrategyWithThreeTargets_with_leverage_1"
      case "SimpleStrategyWithThreeTargetsAndTrailingLoss" =>
        strategiesList += "LeveragedSS3TTL_with_leverage_1_and_parameter_10"
      case "SimpleStrategyWithThreeTargetsAndStoppingLoss" =>
        strategiesList += "LeveragedSS3TSL_with_leverage_1_and_parameter_10"
        
      case "LeveragedSimpleStrategy" =>
        for (leverage: Int <- 1 to 50) {
          strategiesList += "LeveragedSimpleStrategy_with_leverage_" + leverage
        }
      case "LeveragedSimpleLimitedLossStrategy" =>
        for (leverage: Int <- 1 to 50; lossPercentage: Int <- 0 to 75) {
          strategiesList += "LeveragedSimpleLimitedLossStrategy_with_leverage_" + leverage + "_and_lossPercentage_" + lossPercentage
        }
      case "LeveragedSimpleStrategyWithThreeTargets" =>
        for (leverage: Int <- 1 to 50) {
          strategiesList += "LeveragedSimpleStrategyWithThreeTargets_with_leverage_" + leverage
        }
      case "LeveragedSS3TTL" =>
        for (leverage: Int <- 1 to 50; percentage: Int <- 1 to 50) {
          strategiesList += "LeveragedSS3TTL_with_leverage_" + leverage + "_and_percentage_" + percentage
        }
      case "LeveragedSS3TSL" =>
        for (leverage: Int <- 1 to 50; percentage: Int <- 1 to 50) {
          strategiesList += "LeveragedSS3TSL_with_leverage_" + leverage + "_and_percentage_" + percentage
        }
      case "LeveragedSS3TTL2" =>
        for (leverage: Int <- 1 to 50; percentage: Int <- 1 to 20; percentage2: Int <- 0 to 20) {
          strategiesList += "LeveragedSS3TTL2_with_leverage_" + leverage + "_and_percentage_" + percentage + "_and_percentage2_" + percentage2
        }

      case "TrailingLossSimpleStrategy" =>
        for (percentage: Int <- 0 to 400) { //apparently best between 15-20 but with what products?
          strategiesList += "TrailingLossSimpleStrategy_with_percentage_" + percentage + "_and_leverage_1"
        }
      case "LeveragedTrailingLossStrategy" =>
        for (percentage: Int <- 0 to 50; leverage: Int <- 1 to 50) {
          strategiesList += "LeveragedTrailingLossStrategy_with_percentage_" + (percentage).toString + "_and_leverage_" + leverage
        }
      case "test" =>
        strategiesList += "LeveragedSimpleStrategyWithThreeTargets_with_leverage_10"
    }
    strategiesList.toList

  def getStrategyFromName(strategyName: String, signal: Signal): Strategy =
    val parameters: Array[String] = strategyName.split("_")

    if strategyName.startsWith("LeveragedSimpleStrategyWithThreeTargets_") then
      LeveragedSS3T(signal, parameters(3).toInt)
    else if strategyName.startsWith("LeveragedSS3TTL2") then
      LeveragedSS3TTL2(signal, parameters(3).toInt, parameters(6).toInt, parameters(9).toInt)
    else if strategyName.startsWith("LeveragedSS3TTL") then
      LeveragedSS3TTL(signal, parameters(3).toInt, parameters(6).toInt)
    else if strategyName.startsWith("LeveragedSS3TSL") then
      LeveragedSS3TSL(signal, parameters(3).toInt, parameters(6).toInt)
    else if strategyName.startsWith("LeveragedSimpleStrategy_") then
      LeveragedSS(signal, parameters(3).toInt)
    else if strategyName.startsWith("LeveragedSimpleLimitedLossStrategy_") then
      LeveragedSSLL(signal, parameters(3).toInt, parameters(6).toInt)

    else if strategyName.startsWith("LeveragedTrailingLossStrategy_with_percentage_") then
      LeveragedTL(signal, parameters(3).toDouble, parameters(6).toInt)
    else
      println("Strategy name not recognized, returning Simple Strategy")
      LeveragedSS(signal, 1)
}
