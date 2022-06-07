package ch.xavier
package strategy.simple.concrete

import quote.Quote
import signals.Signal
import strategy.simple.SimpleStrategy

import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule, OverIndicatorRule, UnderIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}


class LeveragedSS3TTL(val signal: Signal, override val leverage: Int, val tradingLossPercentage: Int) extends SimpleStrategy {
  val closePriceIndicator: ClosePriceIndicator = ClosePriceIndicator(series)

  val entryPriceReachedRule: Rule = if signal.isLong then OverIndicatorRule(closePriceIndicator, signal.entryPrice)
  else UnderIndicatorRule(closePriceIndicator, signal.entryPrice)

  var stopLossReachedRule: Rule = if signal.isLong then CrossedDownIndicatorRule(closePriceIndicator, signal.stopLoss)
  else CrossedUpIndicatorRule(closePriceIndicator, signal.stopLoss)

  var trailingLossAfterThirdTargetReachedRule: Rule = OverIndicatorRule(closePriceIndicator, 999999999)

  var secondTargetReached = false
  var thirdTargetReached = false

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

        stopLossReachedRule = if signal.isLong then CrossedDownIndicatorRule(closePriceIndicator, liquiditationPrice)
        else CrossedUpIndicatorRule(closePriceIndicator, liquiditationPrice)

      true
    else
      false

  def shouldExit: Boolean = stopLossReachedRule.isSatisfied(series.getEndIndex) || trailingLossAfterThirdTargetReachedRule.isSatisfied(series.getEndIndex)


  override def addQuote(quote: Quote): Unit = 
    super.addQuote(quote)

    if signal.isLong then
      if quote.close > peakPrice then
        peakPrice = quote.close

      if quote.close > signal.thirdTargetPrice then
        thirdTargetReached = true
        stopLossReachedRule = CrossedDownIndicatorRule(closePriceIndicator, signal.thirdTargetPrice)
        trailingLossAfterThirdTargetReachedRule = CrossedDownIndicatorRule(closePriceIndicator, ((100.0 - tradingLossPercentage / 10) * peakPrice) / 100.0)
      else if quote.close > signal.secondTargetPrice && !thirdTargetReached then
        secondTargetReached = true
        stopLossReachedRule = CrossedDownIndicatorRule(closePriceIndicator, signal.secondTargetPrice)
      else if quote.close > signal.firstTargetPrice && !secondTargetReached && !thirdTargetReached then
        stopLossReachedRule = CrossedDownIndicatorRule(closePriceIndicator, signal.firstTargetPrice)
    else
      if quote.close < peakPrice then
        peakPrice = quote.close

      if quote.close < signal.thirdTargetPrice then
        thirdTargetReached = true
        stopLossReachedRule = CrossedUpIndicatorRule(closePriceIndicator, signal.thirdTargetPrice)
        trailingLossAfterThirdTargetReachedRule = CrossedUpIndicatorRule(closePriceIndicator, ((100.0 + tradingLossPercentage / 10) * peakPrice) / 100.0)
      else if quote.close < signal.secondTargetPrice && !thirdTargetReached then
        secondTargetReached = true
        stopLossReachedRule = CrossedUpIndicatorRule(closePriceIndicator, signal.secondTargetPrice)
      else if quote.close < signal.firstTargetPrice && !secondTargetReached && !thirdTargetReached then
        stopLossReachedRule = CrossedUpIndicatorRule(closePriceIndicator, signal.firstTargetPrice)
}
