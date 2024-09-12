package ch.xavier
package strategy

import strategy.simple.*

import akka.NotUsed
import akka.stream.scaladsl.Source
import ch.xavier.strategy.concrete.CrossEMASimpleStrategy
import org.ta4j.core.BarSeries

import scala.::
import scala.collection.mutable.ListBuffer

object StrategiesFactory {

  def getAllStrategiesVariantsNames(strategyNames: List[String]): List[String] =
    var strategiesName: List[String] = List()

    for (strategyName <- strategyNames) do
      strategiesName = strategiesName ++ getStrategieVariantsName(strategyName)

    strategiesName.distinct

  private def getStrategieVariantsName(strategyName: String): List[SimpleStrategy] =
    val strategiesList: ListBuffer[String] = ListBuffer()

    strategyName match {
      case "CrossEMAStrategy" =>
        for (lowerEma: Int <- 5 to 50; upperEma: Int <- 20 to 150) {
          if lowerEma < upperEma then
            strategiesList += s"CrossEMAStrategy_lowerEma_${lowerEma}_upperEma_${upperEma}"
        }
    }

    strategiesList.toList

  def getStrategyFromName(strategyName: String, signal: Signal): SimpleStrategy =
    val parameters: Array[String] = strategyName.split("_")

    if strategyName.startsWith("CrossEMAStrategy") then
      CrossEMAStrategy(signal, parameters(2).toInt, parameters(4).toInt)

}
