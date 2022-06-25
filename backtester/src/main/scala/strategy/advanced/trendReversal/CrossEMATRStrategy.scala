package ch.xavier
package strategy.advanced.trendReversal

import quote.Quote
import signals.Signal
import strategy.advanced.AdvancedStrategy
import strategy.simple.SimpleStrategy

import org.ta4j.core.indicators.{DoubleEMAIndicator, EMAIndicator, HMAIndicator, KAMAIndicator, LWMAIndicator, MMAIndicator, SMAIndicator, TripleEMAIndicator, WMAIndicator, ZLEMAIndicator}
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.*
import org.ta4j.core.{BarSeries, Rule}


class CrossEMATRStrategy(override val leverage: Int, val lowerEma: Int, val upperEma: Int) extends AdvancedStrategy {
  private var shouldBuyLongBool = false
  private var shouldBuyShortBool = false
  private var shouldExitTradeBool = false

  private var hasOpenLongPosition = false
  private var hasOpenShortPosition = false
  private var currentEntryPrice = 0.0
  private var latestClosePrice = 0.0

  private val closePriceIndicator: ClosePriceIndicator = ClosePriceIndicator(series)
  private val lowerEmaIndicator: DoubleEMAIndicator = DoubleEMAIndicator(closePriceIndicator, lowerEma)
  private val upperEmaIndicator: DoubleEMAIndicator = DoubleEMAIndicator(closePriceIndicator, upperEma)

  private val crossedUpIndicatorRule: CrossedUpIndicatorRule = CrossedUpIndicatorRule(lowerEmaIndicator, upperEmaIndicator)
  private val crossedDownIndicatorRule: CrossedDownIndicatorRule = CrossedDownIndicatorRule(lowerEmaIndicator, upperEmaIndicator)

  def shouldEnter: Boolean =
    true

  def shouldExit: Boolean =
    false


  override def addQuote(quote: Quote): Unit =
    super.addQuote(quote)
    latestClosePrice = quote.close

    if crossedUpIndicatorRule.isSatisfied(series.getEndIndex) then
      shouldBuyLongBool = true

      if hasOpenShortPosition then
        shouldExitTradeBool = true

    if crossedDownIndicatorRule.isSatisfied(series.getEndIndex) then
      shouldBuyShortBool = true
      if hasOpenLongPosition then
        shouldExitTradeBool = true

    if hasOpenLongPosition && latestClosePrice < currentEntryPrice then
      shouldExitTradeBool = true

    if hasOpenShortPosition && latestClosePrice > currentEntryPrice then
      shouldExitTradeBool = true


  def shouldExitCurrentTrade: Boolean =
    if shouldExitTradeBool then
      shouldExitTradeBool = false
      hasOpenLongPosition = false
      hasOpenShortPosition = false
      currentEntryPrice = 0.0
      true
    else
      false

  def shouldBuyLong: Boolean =
    if shouldBuyLongBool then
      shouldBuyLongBool = false
      hasOpenLongPosition = true
      currentEntryPrice = latestClosePrice
      true
    else
      false

  def shouldBuyShort: Boolean =
    if shouldBuyShortBool then
      shouldBuyShortBool = false
      hasOpenShortPosition = true
      currentEntryPrice = latestClosePrice
      true
    else
      false
}
