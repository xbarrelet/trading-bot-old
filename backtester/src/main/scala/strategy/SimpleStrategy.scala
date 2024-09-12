package ch.xavier
package strategy

import quote.Quote

import org.ta4j.core.num.DoubleNum
import org.ta4j.core.{BarSeries, BaseBar, BaseBarSeriesBuilder}

import java.time.{Duration, Instant, ZoneId, ZonedDateTime}

trait SimpleStrategy() {
  def shouldEnter: Boolean
  def shouldExit: Boolean
  
  val series: BarSeries = BaseBarSeriesBuilder().withNumTypeOf(DoubleNum.valueOf(_)).build
  def addQuote(quote: Quote): Unit =
    series.addBar(BaseBar.builder()
      .closePrice(DoubleNum.valueOf(quote.close))
      .highPrice(DoubleNum.valueOf(quote.high))
      .openPrice(DoubleNum.valueOf(quote.open))
      .lowPrice(DoubleNum.valueOf(quote.low))
      .timePeriod(Duration.ofMinutes(1))
      .endTime(ZonedDateTime.ofInstant(Instant.ofEpochSecond(quote.start_timestamp + 60), ZoneId.of("UTC")))
      .build())

  val leverage: Int = 1
  def applyLeverageToPercentageGain(percentageGain: Double): Double = percentageGain * leverage
  
  def getName: String
}
