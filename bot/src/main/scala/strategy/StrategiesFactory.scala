package ch.xavier
package strategy

import signals.Signal
import strategy.strats.{LeveragedSimpleStrategy, SimpleStrategy}

import org.ta4j.core.BarSeries

import scala.::
import scala.collection.mutable.ListBuffer

object StrategiesFactory {
  def getStrategyFromName(strategyName: String, signal: Signal): Strategy =
    val parameters: Array[String] = strategyName.split("_")

    if strategyName.equals("SimpleStrategy") then
      SimpleStrategy(signal)
    else if strategyName.startsWith("LeveragedSimpleStrategy") then
      LeveragedSimpleStrategy(signal, parameters(3).toInt)
    else
      println("Strategy name not recognized, returning Simple Strategy")
      SimpleStrategy(signal)
      
  def getCurrentStrategy(signal: Signal): Strategy =
    LeveragedSimpleStrategy(signal, 10)
}
