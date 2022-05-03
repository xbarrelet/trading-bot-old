package ch.xavier
package strategy.strats

import quote.Quote
import signals.Signal
import strategy.Strategy

import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule, OverIndicatorRule, UnderIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}


class LeveragedSimpleStrategyWithThreeTargets(val signal: Signal, override val leverage: Int) extends Strategy {
  //TODO: I should limit the loss to 50% max, no?
  val closePriceIndicator: ClosePriceIndicator = ClosePriceIndicator(series)

  val entryPriceReachedRule: Rule = if signal.isLong then OverIndicatorRule(closePriceIndicator, signal.entryPrice)
  else UnderIndicatorRule(closePriceIndicator, signal.entryPrice)

  var stopLossReachedRule: Rule = if signal.isLong then CrossedDownIndicatorRule(closePriceIndicator, signal.stopLoss)
  else CrossedUpIndicatorRule(closePriceIndicator, signal.stopLoss)


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

  def shouldExit: Boolean = stopLossReachedRule.isSatisfied(series.getEndIndex)

  override def addQuote(quote: Quote): Unit = 
    super.addQuote(quote)

    if signal.isLong then
      if quote.close > signal.thirdTargetPrice then
        stopLossReachedRule = CrossedDownIndicatorRule(closePriceIndicator, signal.thirdTargetPrice)
      else if quote.close > signal.secondTargetPrice then
        stopLossReachedRule = CrossedDownIndicatorRule(closePriceIndicator, signal.secondTargetPrice)
      else if quote.close > signal.firstTargetPrice then
        stopLossReachedRule = CrossedDownIndicatorRule(closePriceIndicator, signal.firstTargetPrice)
    else
      if quote.close < signal.thirdTargetPrice then
        stopLossReachedRule = CrossedUpIndicatorRule(closePriceIndicator, signal.thirdTargetPrice)
      else if quote.close < signal.secondTargetPrice then
        stopLossReachedRule = CrossedUpIndicatorRule(closePriceIndicator, signal.secondTargetPrice)
      else if quote.close < signal.firstTargetPrice then
        stopLossReachedRule = CrossedUpIndicatorRule(closePriceIndicator, signal.firstTargetPrice)
}
