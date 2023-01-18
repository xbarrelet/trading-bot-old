package ch.xavier
package strategy.concrete

import quote.Quote
import signals.Signal
import strategy.AdvancedStrategy

import org.slf4j.{Logger, LoggerFactory}
import org.ta4j.core.indicators.*
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.*
import org.ta4j.core.{BarSeries, Rule}

import java.time.{LocalDateTime, ZoneOffset}


class CrossEMATRStrategy(override val leverage: Int, val lowerEma: Int, val upperEma: Int) extends AdvancedStrategy {
  private val logger: Logger = LoggerFactory.getLogger(f"CrossEMATRStrategy_${lowerEma}_$upperEma")
  
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


  override def addQuote(quote: Quote): Unit =
    latestClosePrice = quote.close

    if series.getBarCount == 0 then
      super.addQuote(quote)
    else
//      if series.getLastBar.getEndTime.plusSeconds(10).toLocalDateTime.isAfter(LocalDateTime.now()) then
//        if hasOpenLongPosition && latestClosePrice < currentEntryPrice then
//          shouldExitTradeBool = true
//
//        if hasOpenShortPosition && latestClosePrice > currentEntryPrice then
//          shouldExitTradeBool = true

      if quote.isFinalQuote then
        super.addQuote(quote)

        if hasOpenLongPosition && latestClosePrice < currentEntryPrice then
          shouldExitTradeBool = true

        if hasOpenShortPosition && latestClosePrice > currentEntryPrice then
          shouldExitTradeBool = true

        if crossedUpIndicatorRule.isSatisfied(series.getEndIndex) then
          shouldBuyLongBool = true
          if hasOpenShortPosition then
            shouldExitTradeBool = true

        if crossedDownIndicatorRule.isSatisfied(series.getEndIndex) then
          shouldBuyShortBool = true
          if hasOpenLongPosition then
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

  override def getName: String =
    "CrossEMATRStrategy_lower_" + lowerEma + "_and_upper_" + upperEma + "_and_leverage_" + leverage

  override def shouldEnter: Boolean = true
  override def shouldExit: Boolean = false

  override def reset: Unit =
    shouldBuyLongBool = false
    shouldBuyShortBool = false
    shouldExitTradeBool = false
}
