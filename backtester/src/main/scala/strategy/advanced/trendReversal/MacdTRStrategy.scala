package ch.xavier
package strategy.advanced.trendReversal

import quote.Quote
import signals.Signal
import strategy.advanced.AdvancedStrategy
import strategy.simple.SimpleStrategy

import org.ta4j.core.indicators.{EMAIndicator, MACDIndicator}
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.indicators.volume.ChaikinMoneyFlowIndicator
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule, OverIndicatorRule, UnderIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}


class MacdTRStrategy(override val leverage: Int) extends AdvancedStrategy {
  //TODO: Why Exception received in BacktesterActor:java.lang.NullPointerException: Cannot invoke "org.ta4j.core.num.Num.isNaN()" because "multiplicand" is null ? With every values
  private var shouldBuyLongBool = false
  private var shouldBuyShortBool = false
  private var shouldExitTradeBool = false

  private var hasOpenLongPosition = false
  private var hasOpenShortPosition = false
  private var currentEntryPrice = 0.0

  private val closePriceIndicator: ClosePriceIndicator = ClosePriceIndicator(series)
  private val macdIndicator: MACDIndicator = MACDIndicator(closePriceIndicator)

  private val crossedUpIndicatorRule: CrossedUpIndicatorRule = CrossedUpIndicatorRule(macdIndicator, 0)
  private val crossedDownIndicatorRule: CrossedDownIndicatorRule = CrossedDownIndicatorRule(macdIndicator, 0)


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
    
  def shouldExitCurrentTrade: Boolean =
    if shouldExitTradeBool then
      shouldExitTradeBool = false
      hasOpenLongPosition = false
      hasOpenShortPosition = false
      currentEntryPrice = 0.0
      true
    else
      false
}
