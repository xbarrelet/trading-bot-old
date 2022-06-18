package ch.xavier
package strategy.advanced.trendReversal

import quote.Quote
import signals.Signal
import strategy.advanced.AdvancedStrategy
import strategy.simple.SimpleStrategy

import org.ta4j.core.indicators.{DoubleEMAIndicator, EMAIndicator}
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.*
import org.ta4j.core.{BarSeries, Rule}


class CrossEMATRWithTLStrategy(override val leverage: Int, percentage: Int) extends AdvancedStrategy {
  private var shouldBuyLongBool = false
  private var shouldBuyShortBool = false
  private var shouldExitTradeBool = false

  private var hasOpenLongPosition = false
  private var hasOpenShortPosition = false
  private var currentEntryPrice = 0.0

  private val closePriceIndicator: ClosePriceIndicator = ClosePriceIndicator(series)
  private val lowerEmaIndicator: DoubleEMAIndicator = DoubleEMAIndicator(closePriceIndicator, 22)
  private val upperEmaIndicator: DoubleEMAIndicator = DoubleEMAIndicator(closePriceIndicator, 166)

  private val crossedUpIndicatorRule: CrossedUpIndicatorRule = CrossedUpIndicatorRule(lowerEmaIndicator, upperEmaIndicator)
  private val crossedDownIndicatorRule: CrossedDownIndicatorRule = CrossedDownIndicatorRule(lowerEmaIndicator, upperEmaIndicator)

  private var highestPrice: Double = 0.0
  private var lowestPrice: Double = 0.0


  def shouldEnter: Boolean =
    true

  def shouldExit: Boolean =
    false


  override def addQuote(quote: Quote): Unit =
    super.addQuote(quote)
    val closePrice = quote.close

    if crossedUpIndicatorRule.isSatisfied(series.getEndIndex) then
      shouldBuyLongBool = true
      currentEntryPrice = closePrice
      if hasOpenShortPosition then
        shouldExitTradeBool = true

    if crossedDownIndicatorRule.isSatisfied(series.getEndIndex) then
      shouldBuyShortBool = true
      currentEntryPrice = closePrice
      if hasOpenLongPosition then
        shouldExitTradeBool = true

    if hasOpenLongPosition then
      if closePrice > highestPrice then
        highestPrice = closePrice

      if closePrice < currentEntryPrice || closePrice < (highestPrice * percentage.toDouble / 10000.0) then
        shouldExitTradeBool = true

    if hasOpenShortPosition then
      if closePrice < lowestPrice then
        lowestPrice = closePrice

      if closePrice > currentEntryPrice then
        shouldExitTradeBool = true


  def shouldExitCurrentTrade: Boolean =
    if shouldExitTradeBool then
      shouldExitTradeBool = false
      hasOpenLongPosition = false
      hasOpenShortPosition = false
      currentEntryPrice = 0.0
      highestPrice = 0.0
      lowestPrice = 0.0
      true
    else
      false

  def shouldBuyLong: Boolean =
    if shouldBuyLongBool then
      shouldBuyLongBool = false
      hasOpenLongPosition = true
      true
    else
      false

  def shouldBuyShort: Boolean =
  if shouldBuyShortBool then
    shouldBuyShortBool = false
    hasOpenShortPosition = true
    true
  else
    false
}
