package ch.xavier
package strategy

import signals.Signal
import strategy.advanced.AdvancedStrategy
import strategy.advanced.concrete.*
import strategy.advanced.trendReversal.*
import strategy.advanced.misc.*
import strategy.simple.*
import strategy.simple.concrete.*

import akka.NotUsed
import akka.stream.scaladsl.Source
import org.ta4j.core.BarSeries

import scala.::
import scala.collection.mutable.ListBuffer

object StrategiesFactory {

  def getAllStrategiesVariantsNames(strategyNames: List[String]): List[String] =
    var names: List[String] = List()

    for (strategyName <- strategyNames) do
      names = names ++ getStrategieVariantsName(strategyName)
      
    names.distinct

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
//      case "LeveragedSimpleStrategyWithThreeTargets" =>
//        for (leverage: Int <- 1 to 50 if leverage % 10 == 0) {
//          strategiesList += "LeveragedSimpleStrategyWithThreeTargets_with_leverage_" + leverage
//        }
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
        for (percentage: Int <- 0 to 50; leverage: Int <- 1 to 50 if leverage % 10 == 0) {
          strategiesList += "LeveragedTrailingLossStrategy_with_percentage_" + percentage.toString + "_and_leverage_" + leverage
        }
      case "LeveragedSS3TFlexibleStoploss" =>
        for (percentage: Int <- 0 to 80; leverage: Int <- 1 to 50 if leverage % 10 == 0) {
          strategiesList += "LeveragedSS3TFlexibleStoploss_with_leverage_" + leverage + "_and_percentage_" + percentage
        }
      case "AdvancedTrailingLossReversalStrat" =>
        strategiesList += "AdvancedTrailingLossReversalStrat"

      case "AdvancedMultiplePositionsLeveragedSS3T" =>
        for (leverage: Int <- 1 to 50  if leverage == 10) {
          strategiesList += "AdvancedMultiplePositionsLeveragedSS3T_with_leverage_" + leverage
        }

      case "CrossEMATRStrategy" =>
        for (lowerEma: Int <- 1 to 50; upperEma: Int <- 150 to 300) {
          strategiesList += "CrossEMATRStrategy_with_lowerEma_" + lowerEma + "_and_upperEma_" + upperEma
        }
      case "CrossEMATRWithTLStrategy" =>
        for (percentage: Int <- 9000 to 10000) {
          strategiesList += "CrossEMATRWithTLStrategy_with_percentage_" + percentage
        }
      case "CrossEMATRWithFixedTLStrategy" =>
        for (value: Int <- 1 to 300) {
          strategiesList += "CrossEMATRWithFixedTLStrategy_with_value_" + value
        }
      case "MFITRStrategy" =>
        for (threshold: Int <- 10 to 99; barcount: Int <- 10 to 60) {
          strategiesList += "MFITRStrategy_with_threshold_" + threshold + "_and_barcount_" + barcount
        }
      case "CCITRStrategy" =>
        for (lowBarCount: Int <- 1 to 50; longBarcount: Int <- 100 to 200 if longBarcount % 10 == 0) {
          strategiesList += "CCITRStrategy_with_lowBarCount_" + lowBarCount + "_and_longBarcount_" + longBarcount
        }
      case "BBTRStrategy" => 
        strategiesList += "BBTRStrategy"
      case "MacdTRStrategy" =>
        strategiesList += "MacdTRStrategy"
      case "SupportAndResistanceStrategy" =>
        for (numberOfBars: Int <- 1 to 3600) {
          strategiesList += "SupportAndResistanceStrategy_with_numberOfBar_" + numberOfBars
        }
    }

    for (leverage: Int <- 1 to 50 if leverage == 10) {
      strategiesList += "LeveragedSimpleStrategyWithThreeTargets_with_leverage_" + leverage
    }

    strategiesList.toList

  def getStrategyFromName(strategyName: String, signal: Signal): SimpleStrategy =
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
    else if strategyName.startsWith("LeveragedSS3TFlexibleStoploss_") then
      LeveragedSS3TFlexibleStoploss(signal, parameters(3).toInt, parameters(6).toInt)
    else
      println("Strategy name not recognized, returning Simple Strategy")
      LeveragedSS(signal, 1)


  def getAdvancedStrategyFromName(strategyName: String, signal: Signal): AdvancedStrategy =
    val parameters: Array[String] = strategyName.split("_")

    if strategyName.startsWith("AdvancedTrailingLossReversalStrat") then
      AdvancedTrailingLossReversalStrat(signal, 10, 1)
    else if strategyName.startsWith("LeveragedSimpleStrategyWithThreeTargets_") then
      AdvancedDefaultLeveragedSS3T(signal, parameters(3).toInt)
    else if strategyName.startsWith("AdvancedMultiplePositionsLeveragedSS3T") then
      AdvancedMultiplePositionsLeveragedSS3T(signal, parameters(3).toInt)
    else if strategyName.startsWith("CrossEMATRStrategy") then
      CrossEMATRStrategy(10, parameters(3).toInt, parameters(6).toInt)
    else if strategyName.startsWith("CrossEMATRWithTLStrategy") then
      CrossEMATRWithTLStrategy(10, parameters(3).toInt)
    else if strategyName.startsWith("CrossEMATRWithFixedTLStrategy") then
      CrossEMATRWithFixedTLStrategy(10, parameters(3).toInt)
    else if strategyName.startsWith("MFITRStrategy") then
      MFITRStrategy(10, parameters(3).toInt, parameters(6).toInt)
    else if strategyName.startsWith("CCITRStrategy") then
      CCITRStrategy(10, parameters(3).toInt, parameters(6).toInt)
    else if strategyName.startsWith("BBTRStrategy") then
      BBTRStrategy(10)
    else if strategyName.startsWith("MacdTRStrategy") then
      MacdTRStrategy(10)
    else if strategyName.startsWith("SupportAndResistanceStrategy") then
      SupportAndResistanceStrategy(10, parameters(3).toInt)
    else
      println("Strategy name not recognized, returning default 3 targets strat")
      AdvancedDefaultLeveragedSS3T(signal, 1)
}
