package ch.xavier
package strategy.simple.concrete

import quote.Quote
import signals.Signal
import strategy.simple.SimpleStrategy

import org.slf4j.{Logger, LoggerFactory}
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.rules.{CrossedDownIndicatorRule, CrossedUpIndicatorRule, OverIndicatorRule, UnderIndicatorRule}
import org.ta4j.core.{BarSeries, Rule}


class LeveragedSS3TFlexibleStoploss(val signal: Signal, override val leverage: Int, acceptedLossPercentage: Int) extends SimpleStrategy {
  val logger: Logger = LoggerFactory.getLogger("LeveragedSS3TFlexibleStoploss")

  var entryPrice: Double = 0.0
  val closePriceIndicator: ClosePriceIndicator = ClosePriceIndicator(series)

  val entryPriceReachedRule: Rule = if signal.isLong then OverIndicatorRule(closePriceIndicator, signal.entryPrice)
  else UnderIndicatorRule(closePriceIndicator, signal.entryPrice)

  var stopLossReachedRule: Rule = if signal.isLong then CrossedDownIndicatorRule(closePriceIndicator, signal.stopLoss)
  else CrossedUpIndicatorRule(closePriceIndicator, signal.stopLoss)

  var acceptedLossReachedRule: Rule = OverIndicatorRule(closePriceIndicator, 999999999)

  var secondTargetReached = false
  var thirdTargetReached = false

  def shouldEnter: Boolean =
    if entryPriceReachedRule.isSatisfied(series.getEndIndex) then
      entryPrice = closePriceIndicator.getValue(series.getEndIndex).doubleValue()

      if leverage != 1 then
        val initialMargin: Double = 1.0 / leverage
        var liquiditationPrice: Double = 0

        if signal.isLong then
          liquiditationPrice = signal.stopLoss.max(entryPrice * (1 - initialMargin))
          acceptedLossReachedRule = CrossedDownIndicatorRule(closePriceIndicator, getIntermediateAmount(liquiditationPrice, entryPrice, acceptedLossPercentage))
        else
          liquiditationPrice = signal.stopLoss.min(entryPrice * (1 + initialMargin))
          acceptedLossReachedRule = CrossedUpIndicatorRule(closePriceIndicator, getIntermediateAmount(entryPrice, liquiditationPrice, acceptedLossPercentage))

        stopLossReachedRule = if signal.isLong then CrossedDownIndicatorRule(closePriceIndicator, liquiditationPrice)
        else CrossedUpIndicatorRule(closePriceIndicator, liquiditationPrice)

      true
    else
      false

  def shouldExit: Boolean =
    stopLossReachedRule.isSatisfied(series.getEndIndex) || acceptedLossReachedRule.isSatisfied(series.getEndIndex)


  override def addQuote(quote: Quote): Unit = 
    super.addQuote(quote)

    if signal.isLong then
      if quote.close > signal.thirdTargetPrice then
        thirdTargetReached = true
        stopLossReachedRule = UnderIndicatorRule(closePriceIndicator, 999999999)
      else if quote.close > signal.secondTargetPrice && !thirdTargetReached then
        secondTargetReached = true
        stopLossReachedRule = UnderIndicatorRule(closePriceIndicator, getIntermediateAmount(signal.firstTargetPrice, signal.secondTargetPrice, acceptedLossPercentage))
      else if quote.close > signal.firstTargetPrice && !secondTargetReached && !thirdTargetReached then
        stopLossReachedRule = UnderIndicatorRule(closePriceIndicator, getIntermediateAmount(entryPrice, signal.firstTargetPrice, acceptedLossPercentage))
    else
      if quote.close < signal.thirdTargetPrice then
        thirdTargetReached = true
        stopLossReachedRule = UnderIndicatorRule(closePriceIndicator, 999999999)
      else if quote.close < signal.secondTargetPrice && !thirdTargetReached then
        secondTargetReached = true
        stopLossReachedRule = OverIndicatorRule(closePriceIndicator, getIntermediateAmount(signal.secondTargetPrice, signal.firstTargetPrice, acceptedLossPercentage))
      else if quote.close < signal.firstTargetPrice && !secondTargetReached && !thirdTargetReached then
        stopLossReachedRule = OverIndicatorRule(closePriceIndicator, getIntermediateAmount(signal.firstTargetPrice, entryPrice, acceptedLossPercentage))


  private def getIntermediateAmount(lowPrice: Double, highPrice: Double, percentage: Int): Double =
    val acceptedLossInterval: Double = (highPrice - lowPrice) * (percentage.toDouble / 100.0)
    highPrice - acceptedLossInterval
}
