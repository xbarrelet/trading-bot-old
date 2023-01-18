package ch.xavier
package strategy.advanced.misc

import quote.Quote
import signals.Signal
import strategy.advanced.AdvancedStrategy
import strategy.simple.SimpleStrategy

import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.indicators.*
import org.ta4j.core.rules.*
import org.ta4j.core.{Bar, BarSeries, Rule}
import scala.jdk.CollectionConverters._

class SupportAndResistanceStrategy(override val leverage: Int, val numberOfQuotes: Int) extends AdvancedStrategy {
  private var shouldBuyLongBool = false
  private var shouldBuyShortBool = false
  private var shouldExitTradeBool = false

  private var hasOpenLongPosition = false
  private var hasOpenShortPosition = false
  private var currentEntryPrice = 0.0
  private var latestClosePrice = 0.0

  //TODO: Profit when you go past the resistance and support but also when you bounce!
  // You should implement graphs here as well for like 1 day to see what is happening
  // First version use the high and min, second you can  use:

  // A fractal is a candlestick pattern made by 5 candles. The third candle has the lowest low price, the previous
  // candles have decreasing lows and the next candles have increasing lows. By this pattern, the low of the third
  // candle is the support level. The same concept applies to resistance levels, where the third candle has the highest
  // high of the five ones.

  def shouldEnter: Boolean =
    true

  def shouldExit: Boolean =
    false


  override def addQuote(quote: Quote): Unit =
    super.addQuote(quote)
    latestClosePrice = quote.close

    if series.getBarCount > numberOfQuotes + 2 then
      val subSeries: BarSeries = series.getSubSeries(series.getEndIndex - (numberOfQuotes + 2), series.getEndIndex - 2)
      var highestPrice: Double = 0
      var lowestPrice: Double = 0

      for bar: Bar <- subSeries.getBarData.asScala do
        if bar.getClosePrice.doubleValue() < lowestPrice then
          lowestPrice = bar.getClosePrice.doubleValue()
        if bar.getClosePrice.doubleValue() > highestPrice then
          highestPrice = bar.getClosePrice.doubleValue()

      if series.getBar(series.getEndIndex - 1).getClosePrice.doubleValue() > highestPrice &&
        series.getLastBar.getClosePrice.doubleValue() < highestPrice then
        shouldExitTradeBool = true

      else if series.getBar(series.getEndIndex - 1).getClosePrice.doubleValue() < lowestPrice &&
        series.getLastBar.getClosePrice.doubleValue() > lowestPrice then
        shouldExitTradeBool = true

      else if series.getLastBar.getClosePrice.doubleValue() > highestPrice then
        shouldBuyLongBool = true

      else if series.getLastBar.getClosePrice.doubleValue() < lowestPrice then
        shouldBuyShortBool = true

  def shouldExitCurrentTrade: Boolean =
    if shouldExitTradeBool then
      shouldExitTradeBool = false
      hasOpenLongPosition = false
      hasOpenShortPosition = false
//      currentEntryPrice = 0.0
      true
    else
      false

  def shouldBuyLong: Boolean =
    if shouldBuyLongBool then
      shouldBuyLongBool = false
      hasOpenLongPosition = true
//      currentEntryPrice = latestClosePrice
      true
    else
      false

  def shouldBuyShort: Boolean =
    if shouldBuyShortBool then
      shouldBuyShortBool = false
      hasOpenShortPosition = true
//      currentEntryPrice = latestClosePrice
      true
    else
      false
}
