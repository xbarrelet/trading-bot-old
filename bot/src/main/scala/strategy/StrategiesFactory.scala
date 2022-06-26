package ch.xavier
package strategy

import signals.Signal

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}
import org.ta4j.core.BarSeries
import strategy.concrete.CrossEMATRStrategy

import scala.::
import scala.collection.mutable.ListBuffer

object StrategiesFactory {
  val logger: Logger = LoggerFactory.getLogger("StrategiesFactory")

  val leverage: Int = DynamicConfig.leverage
  logger.info(s"Starting strategies factory with a leverage of $leverage")


  def getCurrentStrategy(signal: Signal): AdvancedStrategy =
    CrossEMATRStrategy(leverage, 2, 181)

  def getCurrentTRStrategy(lowerDEMA: Int, upperDEMA: Int): AdvancedStrategy =
    CrossEMATRStrategy(leverage, lowerDEMA, upperDEMA)
}
