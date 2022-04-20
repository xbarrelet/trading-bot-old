package ch.xavier
package strategy

import Quote.Quote

import org.ta4j.core.num.DoubleNum
import org.ta4j.core.{BarSeries, BaseBar}

import java.time.{Duration, Instant, ZoneId, ZonedDateTime}

trait Strategy() {
  val series: BarSeries
  
  def addQuote(quote: Quote): Unit =
    series.addBar(BaseBar.builder()
      .closePrice(DoubleNum.valueOf(quote.close))
      .highPrice(DoubleNum.valueOf(quote.high))
      .openPrice(DoubleNum.valueOf(quote.open))
      .lowPrice(DoubleNum.valueOf(quote.low))
      .timePeriod(Duration.ofMinutes(1))
      .endTime(ZonedDateTime.ofInstant(Instant.ofEpochSecond(quote.start_timestamp + 60), ZoneId.of("UTC")))
      .build()
    )

  def shouldEnter: Boolean
  def shouldExit: Boolean
}
