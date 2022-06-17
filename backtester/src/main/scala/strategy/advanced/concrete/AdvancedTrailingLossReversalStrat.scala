package ch.xavier
package strategy.advanced.concrete

import quote.Quote
import signals.Signal
import strategy.simple.SimpleStrategy

import ch.xavier.strategy.advanced.AdvancedStrategy
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule, OverIndicatorRule, UnderIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}


class AdvancedTrailingLossReversalStrat(val signal: Signal, override val leverage: Int, val tradingLossPercentage: Int) extends AdvancedStrategy {
  val closePriceIndicator: ClosePriceIndicator = ClosePriceIndicator(series)

  val entryPriceReachedRule: Rule = if signal.isLong then OverIndicatorRule(closePriceIndicator, signal.entryPrice)
  else UnderIndicatorRule(closePriceIndicator, signal.entryPrice)

  var stopLossReachedRule: Rule = if signal.isLong then CrossedDownIndicatorRule(closePriceIndicator, signal.stopLoss)
  else CrossedUpIndicatorRule(closePriceIndicator, signal.stopLoss)

  var highestPrice = 0.0
  var lowestPrice = 0.0
  var currentTradeEntryPrice = 0.0
  var currentTradeStopLoss = 0.0
  var isCurrentTradeLong = false
  var isCurrentTradeShort = false

  def shouldEnter: Boolean =
    if entryPriceReachedRule.isSatisfied(series.getEndIndex) then
      val entryPrice: Double = closePriceIndicator.getValue(series.getEndIndex).doubleValue()

      if leverage != 1 then
        val initialMargin: Double = 1.0 / leverage
        var liquiditationPrice: Double = 0

        if signal.isLong then
          liquiditationPrice = signal.stopLoss.max(entryPrice * (1 - initialMargin))
          isCurrentTradeLong = true
          currentTradeStopLoss = entryPrice - (tradingLossPercentage * entryPrice / 100)
        else
          liquiditationPrice = signal.stopLoss.min(entryPrice * (1 + initialMargin))
          isCurrentTradeShort = true
          currentTradeStopLoss = entryPrice + (tradingLossPercentage * entryPrice / 100)

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
        stopLossReachedRule = UnderIndicatorRule(closePriceIndicator, 999999999)
    else
      if quote.close < signal.thirdTargetPrice then
        stopLossReachedRule = UnderIndicatorRule(closePriceIndicator, 999999999)

    if isCurrentTradeLong then
      if quote.close > highestPrice then
        highestPrice = quote.close

    if isCurrentTradeShort then
      if quote.close < lowestPrice then
        lowestPrice = quote.close

  def shouldBuyLong: Boolean =
    if !isCurrentTradeLong then
      return true
    false

  def shouldBuyShort: Boolean =
    false

  def shouldExitCurrentTrade: Boolean =
    if isCurrentTradeLong && series.getLastBar.getClosePrice.doubleValue() < currentTradeStopLoss then
      isCurrentTradeLong = false
      return true

    if isCurrentTradeShort && series.getLastBar.getClosePrice.doubleValue() > currentTradeStopLoss then
      isCurrentTradeShort = false
      return true

    false
}
