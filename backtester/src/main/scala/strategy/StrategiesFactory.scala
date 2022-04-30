package ch.xavier
package strategy

import signals.Signal

import akka.NotUsed
import akka.stream.scaladsl.Source
import org.ta4j.core.BarSeries

import scala.::
import scala.collection.mutable.ListBuffer
import ch.xavier.strategy.strats.{LeveragedSimpleStrategy, SimpleStrategy}

object StrategiesFactory {

  def getStrategieVariantsName(strategyName: String): Source[String, NotUsed] =
    val strategiesList: ListBuffer[String] = ListBuffer()

    strategyName match {
      case "SimpleStrategy" =>
        strategiesList += "SimpleStrategy"
      case "LeveragedSimpleStrategy" =>
        for(leverage: Int <- 1 to 50) {
          strategiesList += "LeveragedSimpleStrategy_with_leverage_" + leverage
        }
    }
    Source(strategiesList.toList)

  def getStrategyFromName(strategyName: String, signal: Signal): Strategy =
    val parameters: Array[String] = strategyName.split("_")

    if strategyName.equals("SimpleStrategy") then
      SimpleStrategy(signal)
    else if strategyName.startsWith("LeveragedSimpleStrategy") then
      LeveragedSimpleStrategy(signal, parameters(3).toInt)
    else
      println("Strategy name not recognized, returning Simple Strategy")
      SimpleStrategy(signal)
}
