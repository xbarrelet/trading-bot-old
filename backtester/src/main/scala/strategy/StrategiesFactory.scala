package ch.xavier
package strategy

import signals.Signal

import akka.NotUsed
import akka.stream.scaladsl.Source
import org.ta4j.core.BarSeries

import scala.::
import scala.collection.mutable.ListBuffer
import ch.xavier.strategy.strats.{LeveragedSimpleStrategy, SimpleStrategy, SimpleStrategyWithThreeTargets,
  TrailingLossSimpleStrategy, LeveragedTrailingLossStrategy}

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
        strategiesList += "SimpleStrategy"
      case "SimpleStrategyWithThreeTargets" =>
        strategiesList += "SimpleStrategyWithThreeTargets"
        
      case "LeveragedSimpleStrategy" =>
        for (leverage: Int <- 1 to 50) {
          strategiesList += "LeveragedSimpleStrategy_with_leverage_" + leverage
        }
      case "LeveragedSimpleStrategyWithThreeTargets" =>
        for (leverage: Int <- 1 to 50) {
          strategiesList += "LeveragedSimpleStrategyWithThreeTargets_with_leverage_" + leverage
        }
        
      case "TrailingLossSimpleStrategy" =>
        for (percentage: Int <- 0 to 30) { //apparently best between 15-20 but with what products?
          strategiesList += "TrailingLossSimpleStrategy_with_percentage_" + percentage
        }
      case "LeveragedTrailingLossStrategy" =>
        for (percentage: Int <- 0 to 30; leverage: Int <- 1 to 50) {
          strategiesList += "LeveragedTrailingLossStrategy_with_percentage_" + (percentage / 10.0).toString + "_and_leverage_" + leverage
        }
    }
    strategiesList.toList

  def getStrategyFromName(strategyName: String, signal: Signal): Strategy =
    val parameters: Array[String] = strategyName.split("_")

    if strategyName.equals("SimpleStrategy") then
      SimpleStrategy(signal)
    else if strategyName.equals("SimpleStrategyWithThreeTargets") then
      SimpleStrategyWithThreeTargets(signal)

    else if strategyName.startsWith("LeveragedSimpleStrategyWithThreeTargets") then
      LeveragedSimpleStrategy(signal, parameters(3).toInt)
    else if strategyName.startsWith("LeveragedSimpleStrategy") then
      LeveragedSimpleStrategy(signal, parameters(3).toInt)

    else if strategyName.startsWith("TrailingLossSimpleStrategy") then
      TrailingLossSimpleStrategy(signal, parameters(3).toInt)
    else if strategyName.startsWith("LeveragedTrailingLossStrategy_with_percentage_") then
      LeveragedTrailingLossStrategy(signal, parameters(3).toDouble, parameters(6).toInt)
    else
      println("Strategy name not recognized, returning Simple Strategy")
      SimpleStrategy(signal)
}
