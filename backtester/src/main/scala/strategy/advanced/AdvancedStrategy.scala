package ch.xavier
package strategy.advanced

import quote.Quote

import org.ta4j.core.num.DoubleNum
import org.ta4j.core.{BarSeries, BaseBar, BaseBarSeriesBuilder}

import java.time.{Duration, Instant, ZoneId, ZonedDateTime}

trait AdvancedStrategy() {
  def shouldEnter: Boolean
  def shouldExit: Boolean
  def shouldBuyLong: Boolean
  def shouldBuyShort: Boolean
  
  val series: BarSeries = BaseBarSeriesBuilder().withNumTypeOf(DoubleNum.valueOf(_)).build
  def addQuote(quote: Quote): Unit =
    try {
      series.addBar(BaseBar.builder()
        .closePrice(DoubleNum.valueOf(quote.close))
        .highPrice(DoubleNum.valueOf(quote.high))
        .openPrice(DoubleNum.valueOf(quote.open))
        .lowPrice(DoubleNum.valueOf(quote.low))
        .timePeriod(Duration.ofMinutes(1))
        .endTime(ZonedDateTime.ofInstant(Instant.ofEpochSecond(quote.start_timestamp + 60), ZoneId.of("UTC")))
        .build())
    }
    catch
      case e: IllegalArgumentException => //In case we add the same quote twice

  val leverage: Int = 1
  def applyLeverageToPercentageGain(percentageGain: Double): Double = percentageGain * leverage
}
