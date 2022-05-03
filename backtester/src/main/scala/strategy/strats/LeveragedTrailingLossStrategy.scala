package ch.xavier
package strategy.strats

import quote.Quote
import signals.Signal
import strategy.Strategy

import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule, OverIndicatorRule, UnderIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}


class LeveragedTrailingLossStrategy(val signal: Signal, percentage: Double, override val leverage: Int) extends Strategy {
  val closePriceIndicator: ClosePriceIndicator = ClosePriceIndicator(series)

  val entryPriceReachedRule: Rule = if signal.isLong then OverIndicatorRule(closePriceIndicator, signal.entryPrice)
  else UnderIndicatorRule(closePriceIndicator, signal.entryPrice)

  var stopLossReachedRule: Rule = if signal.isLong then CrossedDownIndicatorRule(closePriceIndicator, signal.stopLoss)
  else CrossedUpIndicatorRule(closePriceIndicator, signal.stopLoss)

  val firstTargetReachedRule: Rule = if signal.isLong then CrossedUpIndicatorRule(closePriceIndicator, signal.firstTargetPrice)
  else CrossedDownIndicatorRule(closePriceIndicator, signal.firstTargetPrice)

  var peakPrice: Double = signal.entryPrice


  def shouldEnter: Boolean =
    entryPriceReachedRule.isSatisfied(series.getEndIndex)

  def shouldExit: Boolean =
    stopLossReachedRule.isSatisfied(series.getEndIndex)

  override def addQuote(quote: Quote): Unit =
    super.addQuote(quote)

    if signal.isLong then
      if quote.close > peakPrice then
        peakPrice = quote.close
        stopLossReachedRule = CrossedDownIndicatorRule(closePriceIndicator, (((100.0 - percentage) * leverage) * peakPrice) / 100.0 )
    else
      if quote.close < peakPrice then
        peakPrice = quote.close
        stopLossReachedRule = CrossedUpIndicatorRule(closePriceIndicator, (((100.0 + percentage) * leverage) * peakPrice) / 100.0 )
}
