package ch.xavier
package strategy.concrete

import quote.Quote
import signals.Signal
import strategy.Strategy

import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule, OverIndicatorRule, UnderIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}


class LeveragedTL(val signal: Signal, percentage: Double, override val leverage: Int) extends Strategy {
  val closePriceIndicator: ClosePriceIndicator = ClosePriceIndicator(series)

  val entryPriceReachedRule: Rule = if signal.isLong then OverIndicatorRule(closePriceIndicator, signal.entryPrice)
  else UnderIndicatorRule(closePriceIndicator, signal.entryPrice)

  val stopLossReachedRule: Rule = if signal.isLong then CrossedDownIndicatorRule(closePriceIndicator, signal.stopLoss)
  else CrossedUpIndicatorRule(closePriceIndicator, signal.stopLoss)

  var thresholdCrossedRule: Rule = if signal.isLong then CrossedDownIndicatorRule(closePriceIndicator, signal.stopLoss)
  else CrossedUpIndicatorRule(closePriceIndicator, signal.stopLoss)

  var liquidityPriceReachedRule: Rule = null

  var peakPrice: Double = signal.entryPrice


  def shouldEnter: Boolean =
    if entryPriceReachedRule.isSatisfied(series.getEndIndex) then
      val entryPrice: Double = closePriceIndicator.getValue(series.getEndIndex).doubleValue()

      if leverage != 1 then
        val initialMargin: Double = 1.0 / leverage
        var liquiditationPrice: Double = 0

        if signal.isLong then
          liquiditationPrice = signal.stopLoss.max(entryPrice * (1 - initialMargin))
        else
          liquiditationPrice = signal.stopLoss.min(entryPrice * (1 + initialMargin))

        liquidityPriceReachedRule = if signal.isLong then CrossedDownIndicatorRule(closePriceIndicator, liquiditationPrice)
        else CrossedUpIndicatorRule(closePriceIndicator, liquiditationPrice)

      true
    else
      false

  def shouldExit: Boolean =
    stopLossReachedRule.isSatisfied(series.getEndIndex) ||
      thresholdCrossedRule.isSatisfied(series.getEndIndex) ||
      (liquidityPriceReachedRule != null && liquidityPriceReachedRule.isSatisfied(series.getEndIndex))

  override def addQuote(quote: Quote): Unit =
    super.addQuote(quote)

    if signal.isLong then
      if quote.close > peakPrice then
        peakPrice = quote.close
        thresholdCrossedRule = CrossedDownIndicatorRule(closePriceIndicator, ((100.0 - percentage / 10) * peakPrice) / 100.0)
    else
      if quote.close < peakPrice then
        peakPrice = quote.close
        thresholdCrossedRule = CrossedUpIndicatorRule(closePriceIndicator, ((100.0 + percentage / 10) * peakPrice) / 100.0)
}
