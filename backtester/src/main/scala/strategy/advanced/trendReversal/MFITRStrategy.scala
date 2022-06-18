package ch.xavier
package strategy.advanced.trendReversal

import quote.Quote
import signals.Signal
import strategy.advanced.AdvancedStrategy
import strategy.simple.SimpleStrategy

import org.ta4j.core.indicators.EMAIndicator
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.indicators.volume.ChaikinMoneyFlowIndicator
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule, OverIndicatorRule, UnderIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}


class MFITRStrategy(override val leverage: Int, val threshold: Int, val barCount: Int) extends AdvancedStrategy {
  //TODO: Why Exception received in BacktesterActor:java.lang.NullPointerException: Cannot invoke "org.ta4j.core.num.Num.isNaN()" because "multiplicand" is null ? With every values
  private var shouldBuyLongBool = false
  private var shouldBuyShortBool = false
  private var shouldExitTradeBool = false

  private var hasOpenLongPosition = false
  private var hasOpenShortPosition = false
  private var currentEntryPrice = 0.0

  private val mFindicator: ChaikinMoneyFlowIndicator = ChaikinMoneyFlowIndicator(series, barCount)

  private val positiveThreshold = series.numOf(threshold.toDouble / 100.0)
  private val negativeThreshold = series.numOf(-1 * threshold.toDouble / 100.0)
  private val crossedUpIndicatorRule: CrossedUpIndicatorRule = CrossedUpIndicatorRule(mFindicator, positiveThreshold)
  private val crossedDownIndicatorRule: CrossedDownIndicatorRule = CrossedDownIndicatorRule(mFindicator, negativeThreshold)


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
