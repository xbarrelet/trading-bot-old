package ch.xavier
package strategy.strats

import quote.Quote
import signals.Signal
import strategy.Strategy

import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule, OverIndicatorRule, UnderIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}

//TODO: Do one with a switch of strat after first target?
class SimpleStrategyWithThreeTargets(val signal: Signal) extends Strategy {
  val closePriceIndicator: ClosePriceIndicator = ClosePriceIndicator(series)

  val entryPriceReachedRule: Rule = if signal.isLong then OverIndicatorRule(closePriceIndicator, signal.entryPrice)
  else UnderIndicatorRule(closePriceIndicator, signal.entryPrice)

  var stopLossReachedRule: Rule = if signal.isLong then CrossedDownIndicatorRule(closePriceIndicator, signal.stopLoss)
  else CrossedUpIndicatorRule(closePriceIndicator, signal.stopLoss)


  def shouldEnter: Boolean =
    entryPriceReachedRule.isSatisfied(series.getEndIndex)

  def shouldExit: Boolean =
    stopLossReachedRule.isSatisfied(series.getEndIndex)

  override def addQuote(quote: Quote): Unit = 
    super.addQuote(quote)
    
    if signal.isLong then
      if quote.close > signal.thirdTargetPrice then
        stopLossReachedRule = UnderIndicatorRule(closePriceIndicator, 99999999)
      else if quote.close > signal.secondTargetPrice then
        stopLossReachedRule = CrossedDownIndicatorRule(closePriceIndicator, signal.secondTargetPrice)
      else if quote.close > signal.firstTargetPrice then
        stopLossReachedRule = CrossedDownIndicatorRule(closePriceIndicator, signal.firstTargetPrice)
    else
      if quote.close < signal.thirdTargetPrice then
        stopLossReachedRule = UnderIndicatorRule(closePriceIndicator, 99999999)
      else if quote.close < signal.secondTargetPrice then
        stopLossReachedRule = CrossedUpIndicatorRule(closePriceIndicator, signal.secondTargetPrice)
      else if quote.close < signal.firstTargetPrice then
        stopLossReachedRule = CrossedUpIndicatorRule(closePriceIndicator, signal.firstTargetPrice)
}
