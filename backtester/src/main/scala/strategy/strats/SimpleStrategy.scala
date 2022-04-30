package ch.xavier
package strategy.strats

import Quote.Quote
import signals.Signal
import strategy.Strategy

import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule, OverIndicatorRule, UnderIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}


class SimpleStrategy(val signal: Signal) extends Strategy {
  val closePriceIndicator: ClosePriceIndicator = ClosePriceIndicator(series)

  val entryPriceReachedRule: Rule = if signal.isLong then OverIndicatorRule(closePriceIndicator, signal.entryPrice)
  else UnderIndicatorRule(closePriceIndicator, signal.entryPrice)

  val stopLossReachedRule: Rule = if signal.isLong then CrossedDownIndicatorRule(closePriceIndicator, signal.stopLoss)
  else CrossedUpIndicatorRule(closePriceIndicator, signal.stopLoss)

  val firstTargetReachedRule: Rule = if signal.isLong then CrossedUpIndicatorRule(closePriceIndicator, signal.firstTargetPrice)
  else CrossedDownIndicatorRule(closePriceIndicator, signal.firstTargetPrice)


  def shouldEnter: Boolean =
    entryPriceReachedRule.isSatisfied(series.getEndIndex)

  def shouldExit: Boolean =
    firstTargetReachedRule.isSatisfied(series.getEndIndex) || stopLossReachedRule.isSatisfied(series.getEndIndex)
}
