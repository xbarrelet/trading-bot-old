package ch.xavier
package strategy.advanced.concrete

import quote.Quote
import signals.Signal
import strategy.advanced.AdvancedStrategy
import strategy.simple.SimpleStrategy

import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule, OverIndicatorRule, UnderIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}


class AdvancedMultiplePositionsLeveragedSS3T(val signal: Signal, override val leverage: Int) extends AdvancedStrategy {
  private var shouldBuyLongBool = false
  private var shouldBuyShortBool = false
  private var shouldExitTradeBool = false
  private var shouldEnterBool = false
  private var shouldExitBool = false

  private var hasEntered = false
  private var hasOpenPosition = false
  private var entryPriceReached = false
  private var firstTargetReached = false
  private var secondTargetReached = false

  private var currentEntryPrice = 0.0

  private val isLong = signal.isLong


  def shouldEnter: Boolean =
    if shouldEnterBool then
      shouldEnterBool = false
      hasEntered = true
      true
    else
      false

  def shouldExit: Boolean =
    if shouldExitBool then
      true
    else
      false


  override def addQuote(quote: Quote): Unit =
    super.addQuote(quote)
    val close: Double = quote.close

    if !hasEntered then
      if isPriceAbove(close, signal.entryPrice) then
        shouldEnterBool = true
        entryPriceReached = true
        hasOpenPosition = true
        currentEntryPrice = close
    else

      if !hasOpenPosition && !entryPriceReached && isPriceAbove(close, signal.entryPrice) then
        buyInSenseOfSignal()
        hasOpenPosition = true
        entryPriceReached = true
        currentEntryPrice = close

      if hasOpenPosition && !firstTargetReached && isPriceBelow(close, currentEntryPrice) then
        shouldExitTradeBool = true
        hasOpenPosition = false
        entryPriceReached = false
        currentEntryPrice = 0.0

      else if !hasOpenPosition && entryPriceReached && isPriceAbove(close, signal.firstTargetPrice) then
        buyInSenseOfSignal()
        hasOpenPosition = true
        firstTargetReached = true
        currentEntryPrice = close

      else if hasOpenPosition && firstTargetReached && isPriceBelow(close, signal.firstTargetPrice) then
        shouldExitTradeBool = true
        hasOpenPosition = false
        firstTargetReached = false
        currentEntryPrice = 0.0

      else if !hasOpenPosition && firstTargetReached && isPriceAbove(close, signal.secondTargetPrice) then
        buyInSenseOfSignal()
        hasOpenPosition = true
        secondTargetReached = true
        currentEntryPrice = close

      else if hasOpenPosition && secondTargetReached && isPriceBelow(close, signal.secondTargetPrice) then
        shouldExitTradeBool = true
        hasOpenPosition = false
        secondTargetReached = false
        currentEntryPrice = 0.0

      else if isPriceAbove(close, signal.thirdTargetPrice) then
        shouldExitBool = true

  def shouldBuyLong: Boolean =
    if shouldBuyLongBool then
      shouldBuyLongBool = false
      true
    else
      false

  def shouldBuyShort: Boolean =
  if shouldBuyShortBool then
    shouldBuyShortBool = false
    true
  else
    false
    
  def shouldExitCurrentTrade: Boolean =
    if shouldExitTradeBool then
      shouldExitTradeBool = false
      true
    else
      false

  //in trait?
  private def isPriceBelow(price: Double, limit: Double): Boolean =
    if isLong then
      price < limit
    else
      price > limit

  private def isPriceAbove(price: Double, limit: Double): Boolean =
    !isPriceBelow(price, limit)

  private def buyInSenseOfSignal(): Unit =
    if isLong then
      shouldBuyLongBool = true
    else
      shouldBuyShortBool = true
}
