package ch.xavier
package strategy

import akka.NotUsed
import akka.stream.scaladsl.Source
import ch.xavier.signals.Signal
import org.ta4j.core.BarSeries

import scala.::
import scala.collection.mutable.ListBuffer


object StrategiesFactory {

  def getStrategieVariantsName(strategyName: String): Source[String, NotUsed] =
    val strategiesList: ListBuffer[String] = ListBuffer()

    strategyName match {
      case "SimpleStrategy" =>
        strategiesList += "SimpleStrategy"
//        for(barCount: Int <- 1 to 5; rsiThreshold: Int <- 20 until 40) {
//          strategiesList += "RSIStrategy_with_barCount_" + barCount + "_and_rsiThreshold_" + rsiThreshold
//        }
    }
    Source(strategiesList.toList)

  def getStrategyFromName(strategyName: String, series: BarSeries, signal: Signal): Strategy =
    val parameters: Array[String] = strategyName.split("_")

    if strategyName.equals("SimpleStrategy") then
      SimpleStrategy(series, signal)
    else
      println("Strategy name not recognized")
      SimpleStrategy(series, signal)
}
