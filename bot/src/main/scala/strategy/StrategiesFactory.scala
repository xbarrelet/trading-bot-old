package ch.xavier
package strategy

import signals.Signal
import strategy.concrete.LeveragedSimpleStrategyWithThreeTargets

import org.ta4j.core.BarSeries

import scala.::
import scala.collection.mutable.ListBuffer

object StrategiesFactory {
  def getCurrentStrategy(signal: Signal): Strategy =
    LeveragedSimpleStrategyWithThreeTargets(signal, 10)
}
