package ch.xavier
package strategy.strats

import quote.Quote
import signals.Signal
import strategy.Strategy

import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule, OverIndicatorRule, UnderIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}


class TrailingLossSimpleStrategy(val signal: Signal, percentage: Int) extends Strategy {
  //TODO: Debug me, I always return the same gain
  //You should use a trailing loss + the original stoploss
  // at the end you'll get a mix of trailing loss + liquidation price + stoploss
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
    firstTargetReachedRule.isSatisfied(series.getEndIndex) || stopLossReachedRule.isSatisfied(series.getEndIndex)

  override def addQuote(quote: Quote): Unit =
    super.addQuote(quote)

    if signal.isLong then
      if quote.close > peakPrice then
        peakPrice = quote.close
        stopLossReachedRule = CrossedDownIndicatorRule(closePriceIndicator, ((100 - percentage) * peakPrice) / 100 )
    else
      if quote.close < peakPrice then
        peakPrice = quote.close
        stopLossReachedRule = CrossedUpIndicatorRule(closePriceIndicator, ((100 + percentage) * peakPrice) / 100 )
}
