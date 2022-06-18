package ch.xavier
package strategy.advanced.trendReversal

import quote.Quote
import signals.Signal
import strategy.advanced.AdvancedStrategy
import strategy.simple.SimpleStrategy

import org.ta4j.core.indicators.{CCIIndicator, EMAIndicator}
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.num.Num
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule, OverIndicatorRule, UnderIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}


class CCITRStrategy(override val leverage: Int, val shortBarCount: Int, val longBarCount: Int) extends AdvancedStrategy {
  private var shouldBuyLongBool = false
  private var shouldBuyShortBool = false
  private var shouldExitTradeBool = false

  private var hasOpenLongPosition = false
  private var hasOpenShortPosition = false
  private var currentEntryPrice = 0.0
  
  private val longCci = new CCIIndicator(series, longBarCount)
  private val shortCci = new CCIIndicator(series, shortBarCount)
  private val plus100: Num = series.numOf(100)
  private val minus100: Num = series.numOf(-100)
  
  private val crossedUpIndicatorRule: Rule = CrossedUpIndicatorRule(longCci, plus100).and(UnderIndicatorRule(shortCci, minus100))
  private val crossedDownIndicatorRule: Rule = CrossedDownIndicatorRule(longCci, minus100).and(OverIndicatorRule(shortCci, plus100))


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
