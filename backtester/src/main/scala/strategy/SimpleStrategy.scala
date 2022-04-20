package ch.xavier
package strategy

import Quote.Quote

import ch.xavier.signals.Signal
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}


class SimpleStrategy(override val series: BarSeries, val signal: Signal) extends Strategy {
  
  val closePriceIndicator: ClosePriceIndicator = ClosePriceIndicator(series)

  val buyEntryPriceReachedRule: Rule = CrossedUpIndicatorRule(closePriceIndicator, signal.entryPrice)
  val sellEntryPriceReachedRule: Rule = CrossedDownIndicatorRule(closePriceIndicator, signal.entryPrice)

  val buyStopLossReachedRule: Rule = CrossedDownIndicatorRule(closePriceIndicator, signal.stopLoss)
  val sellStopLossReachedRule: Rule = CrossedUpIndicatorRule(closePriceIndicator, signal.stopLoss)

  val buyFirstTargetReachedRule: Rule = CrossedUpIndicatorRule(closePriceIndicator, signal.firstTargetPrice)
  val sellFirstTargetReachedRule: Rule = CrossedDownIndicatorRule(closePriceIndicator, signal.firstTargetPrice)


  def shouldEnter: Boolean =
    val lastIndex = series.getEndIndex
    buyEntryPriceReachedRule.isSatisfied(lastIndex) || sellEntryPriceReachedRule.isSatisfied(lastIndex)

  def shouldExit: Boolean =
    val lastIndex = series.getEndIndex
    buyFirstTargetReachedRule.isSatisfied(lastIndex) || sellFirstTargetReachedRule.isSatisfied(lastIndex) ||
      buyStopLossReachedRule.isSatisfied(lastIndex) || sellStopLossReachedRule.isSatisfied(lastIndex)

}
