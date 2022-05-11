package ch.xavier
package strategy

import signals.Signal
import strategy.concrete.LeveragedSS3TTL

import org.ta4j.core.BarSeries

import scala.::
import scala.collection.mutable.ListBuffer

object StrategiesFactory {
  def getCurrentStrategy(signal: Signal): Strategy =
    LeveragedSS3TTL(signal, 10, 1)
}
