package strategy.concrete

import ch.xavier.quote.Quote
import ch.xavier.signals.Signal
import ch.xavier.strategy.simple.SimpleStrategy
import ch.xavier.strategy.simple.concrete.LeveragedSSLL
import org.scalatest.flatspec.AnyFlatSpec
import strategy.StrategySpec

class LeveragedLLSpec extends StrategySpec {
  val testLongSignal: Signal = Signal(
    100, // entry
    150, 200, 250, //3 targets
    true, //Long
    50, // SL
    "TEST_SYMBOL", 1640995200)

  val testShortSignal: Signal = Signal(
    200, // entry
    150, 100, 50, //3 targets
    false, //Long
    250, // SL
    "TEST_SYMBOL", 1640995200)


  behavior of "LeveragedLimitedLossStrategy"


  it should "LONG: strategy exits if price immediately goes under 3% of entry price" in {
    // GIVEN
    val testedStrategy: SimpleStrategy = LeveragedSSLL(testLongSignal, 3, 1)

    val entryQuote: Quote = createQuoteWithPrice(101)
    val stayAboveThreshold = createQuoteWithPrice(98)
    val goesUnderThresholdQuote = createQuoteWithPrice(97)

    testedStrategy.addQuote(entryQuote)

    // WHEN1
    testedStrategy.addQuote(stayAboveThreshold)
    // THEN1
    assert(!testedStrategy.shouldExit)

    // WHEN2
    testedStrategy.addQuote(goesUnderThresholdQuote)
    // THEN2
    assert(testedStrategy.shouldExit)
  }

  it should "SHORT: strategy exits if price immediately goes above 3% of entry price" in {
    // GIVEN
    val testedStrategy: SimpleStrategy = LeveragedSSLL(testShortSignal, 3, 1)

    val entryQuote: Quote = createQuoteWithPrice(199)
    val stayUnderThreshold = createQuoteWithPrice(204)
    val goesAboveThreshold = createQuoteWithPrice(205)

    testedStrategy.addQuote(entryQuote)

    // WHEN1
    testedStrategy.addQuote(stayUnderThreshold)
    // THEN1
    assert(!testedStrategy.shouldExit)

    // WHEN2
    testedStrategy.addQuote(goesAboveThreshold)
    // THEN2
    assert(testedStrategy.shouldExit)
  }

  it should "LONG: strategy exits if price rises up then goes under 3% of peak price" in {
    // GIVEN
    val testedStrategy: SimpleStrategy = LeveragedSSLL(testLongSignal, 3, 1)

    val entryQuote: Quote = createQuoteWithPrice(101)
    val peakPriceQuote: Quote = createQuoteWithPrice(150)
    val stayAboveThreshold = createQuoteWithPrice(146)
    val goesUnderThresholdQuote = createQuoteWithPrice(145)

    testedStrategy.addQuote(entryQuote)
    testedStrategy.addQuote(peakPriceQuote)

    // WHEN1
    testedStrategy.addQuote(stayAboveThreshold)
    // THEN1
    assert(!testedStrategy.shouldExit)

    // WHEN2
    testedStrategy.addQuote(goesUnderThresholdQuote)
    // THEN2
    assert(testedStrategy.shouldExit)
  }

  it should "SHORT: strategy exits if price rises up then goes above 3% of peak price" in {
    // GIVEN
    val testedStrategy: SimpleStrategy = LeveragedSSLL(testShortSignal, 3, 1)

    val entryQuote: Quote = createQuoteWithPrice(199)
    val peakPriceQuote: Quote = createQuoteWithPrice(150)
    val stayUnderThreshold = createQuoteWithPrice(154)
    val goesAboveThreshold = createQuoteWithPrice(155)

    testedStrategy.addQuote(entryQuote)
    testedStrategy.addQuote(peakPriceQuote)

    // WHEN1
    testedStrategy.addQuote(stayUnderThreshold)
    // THEN1
    assert(!testedStrategy.shouldExit)

    // WHEN2
    testedStrategy.addQuote(goesAboveThreshold)
    // THEN2
    assert(testedStrategy.shouldExit)
  }

  it should "LONG: strategy exits if stoploss reached without trailing loss rule triggered" in {
    // GIVEN
    val testSignal: Signal = Signal(
      100, // entry
      150, 200, 250, //3 targets
      true, //Long
      99, // SL
      "TEST_SYMBOL", 1640995200)
    val testedStrategy: SimpleStrategy = LeveragedSSLL(testSignal, 3, 1)

    val entryQuote: Quote = createQuoteWithPrice(101)
    val goesUnderStoplossQuote = createQuoteWithPrice(98)

    testedStrategy.addQuote(entryQuote)

    // WHEN
    testedStrategy.addQuote(goesUnderStoplossQuote)

    // THEN
    assert(testedStrategy.shouldExit)
  }

  it should "SHORT: strategy exits if stoploss reached without trailing loss rule triggered" in {
    val testSignal: Signal = Signal(
      200, // entry
      150, 100, 50, //3 targets
      false, //Long
      201, // SL
      "TEST_SYMBOL", 1640995200)
    val testedStrategy: SimpleStrategy = LeveragedSSLL(testSignal, 3, 1)

    val entryQuote: Quote = createQuoteWithPrice(199)
    val goesAboveStoplossQuote = createQuoteWithPrice(202)

    testedStrategy.addQuote(entryQuote)

    // WHEN
    testedStrategy.addQuote(goesAboveStoplossQuote)

    // THEN
    assert(testedStrategy.shouldExit)
  }
}
