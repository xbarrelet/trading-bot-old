package ch.xavier
package strategy.advanced.trendReversal

import quote.Quote
import signals.Signal
import strategy.advanced.AdvancedStrategy
import strategy.simple.SimpleStrategy

import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.indicators.*
import org.ta4j.core.indicators.bollinger.{BollingerBandsLowerIndicator, BollingerBandsMiddleIndicator, BollingerBandsUpperIndicator}
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator
import org.ta4j.core.rules.*
import org.ta4j.core.{BarSeries, Rule}


class BBTRStrategy(override val leverage: Int) extends AdvancedStrategy {
  private var shouldBuyLongBool = false
  private var shouldBuyShortBool = false
  private var shouldExitTradeBool = false

  private var hasOpenLongPosition = false
  private var hasOpenShortPosition = false
  private var currentEntryPrice = 0.0
  private var currentEntryIndex = 0

  //TODO: Fix me, not businessly correct
  private val closePriceIndicator: ClosePriceIndicator = ClosePriceIndicator(series)
  private val sMAIndicator: SMAIndicator = SMAIndicator(closePriceIndicator, 20)
  private val middleBBIndicator: BollingerBandsMiddleIndicator = BollingerBandsMiddleIndicator(sMAIndicator)
  private val upperBBIndicator: BollingerBandsUpperIndicator = BollingerBandsUpperIndicator(middleBBIndicator, StandardDeviationIndicator(middleBBIndicator, 2))
  private val lowerBBIndicator: BollingerBandsLowerIndicator = BollingerBandsLowerIndicator(middleBBIndicator, StandardDeviationIndicator(middleBBIndicator, 2))
  
  private val crossedUpIndicatorRule: CrossedUpIndicatorRule = CrossedUpIndicatorRule(closePriceIndicator, lowerBBIndicator)
  private val crossedDownIndicatorRule: CrossedDownIndicatorRule = CrossedDownIndicatorRule(closePriceIndicator, upperBBIndicator)


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

    if hasOpenLongPosition && closePrice < currentEntryPrice then
      shouldExitTradeBool = true

    if hasOpenShortPosition && closePrice > currentEntryPrice then
      shouldExitTradeBool = true


  def shouldExitCurrentTrade: Boolean =
    if shouldExitTradeBool then
      shouldExitTradeBool = false
      hasOpenLongPosition = false
      hasOpenShortPosition = false
      currentEntryPrice = 0.0
      currentEntryIndex = 0
      true
    else
      false

  def shouldBuyLong: Boolean =
    if shouldBuyLongBool then
      shouldBuyLongBool = false
      hasOpenLongPosition = true
      currentEntryIndex = series.getEndIndex
      true
    else
      false

  def shouldBuyShort: Boolean =
  if shouldBuyShortBool then
    shouldBuyShortBool = false
    hasOpenShortPosition = true
    currentEntryIndex = series.getEndIndex
    true
  else
    false
}
