package ch.xavier
package strategy

import Quote.Quote

import org.ta4j.core.num.DoubleNum
import org.ta4j.core.{BarSeries, BaseBar}

import java.time.{Duration, Instant, ZoneId, ZonedDateTime}

trait Strategy() {
  val series: BarSeries

  def shouldEnter(index: Int): Boolean
  def shouldExit(index: Int): Boolean
}
