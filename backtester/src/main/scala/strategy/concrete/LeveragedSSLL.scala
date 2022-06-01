package ch.xavier
package strategy.concrete

import quote.Quote
import signals.Signal
import strategy.Strategy

import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule, OverIndicatorRule, UnderIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}


class LeveragedSSLL(val signal: Signal, override val leverage: Int, acceptedLossPercentage: Int) extends Strategy {
  val closePriceIndicator: ClosePriceIndicator = ClosePriceIndicator(series)

  //ENTRY RULE
  val entryPriceReachedRule: Rule = if signal.isLong then OverIndicatorRule(closePriceIndicator, signal.entryPrice)
  else UnderIndicatorRule(closePriceIndicator, signal.entryPrice)

  //EXIT RULES
  val firstTargetReachedRule: Rule = if signal.isLong then CrossedUpIndicatorRule(closePriceIndicator, signal.secondTargetPrice)
  else CrossedDownIndicatorRule(closePriceIndicator, signal.firstTargetPrice)

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
          val acceptedLossInterval = (entryPrice - liquiditationPrice) * (acceptedLossPercentage / 100)
          stopLossReachedRule = CrossedDownIndicatorRule(closePriceIndicator, entryPrice - acceptedLossInterval)
        else
          liquiditationPrice = signal.stopLoss.min(entryPrice * (1 + initialMargin))
          val acceptedLossInterval = (liquiditationPrice - entryPrice) * (acceptedLossPercentage / 100)
          stopLossReachedRule = CrossedUpIndicatorRule(closePriceIndicator, entryPrice + acceptedLossInterval)
      true
    else
      false

  def shouldExit: Boolean =
    firstTargetReachedRule.isSatisfied(series.getEndIndex) || stopLossReachedRule.isSatisfied(series.getEndIndex)
}
