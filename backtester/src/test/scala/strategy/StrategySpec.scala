package strategy

import ch.xavier.quote.Quote
import org.scalatest.flatspec.AnyFlatSpec

class StrategySpec extends AnyFlatSpec {
  var ongoingTimestamp: Long = 1640995200

  def createQuoteWithPrice(price: Double): Quote =
    ongoingTimestamp += 100
    Quote(price, 0.0, 0.0, 0.0, ongoingTimestamp, "TEST_SYMBOL")
}
